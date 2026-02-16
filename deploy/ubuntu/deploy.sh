#!/bin/bash
# =============================================================================
# Performance Portal - Ubuntu 22.04 Deployment Script (From Scratch)
# =============================================================================
# This script installs all prerequisites and deploys the Performance Portal
# on a fresh Ubuntu 22.04 LTS server.
#
# Usage:
#   chmod +x deploy.sh
#   sudo ./deploy.sh
#
# Prerequisites: Fresh Ubuntu 22.04 with internet access and root/sudo
# =============================================================================

set -euo pipefail

# --- Configuration -----------------------------------------------------------
APP_NAME="perf-portal"
APP_DIR="/opt/performance-portal"
DATA_DIR="/opt/performance-portal/data"
REPO_DIR="/opt/performance-portal/src"
POSTGRES_DB="performance_portal"
POSTGRES_USER="postgres"
# Generate strong random password for PostgreSQL
POSTGRES_PASSWORD=$(openssl rand -base64 32)
APP_USER="perf-portal"
BACKEND_PORT=8080
FRONTEND_PORT=3000
JAVA_OPTS="-Xmx1g -Xms512m"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()   { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# --- Pre-flight checks -------------------------------------------------------
if [[ $EUID -ne 0 ]]; then
    error "This script must be run as root (sudo ./deploy.sh)"
fi

log "Starting Performance Portal deployment on Ubuntu 22.04..."

# --- Step 1: System Update ----------------------------------------------------
log "Step 1/8: Updating system packages..."
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get upgrade -y -qq
apt-get install -y -qq curl wget git unzip tar software-properties-common ufw

# --- Step 1.5: Create Application User ----------------------------------------
log "Step 1.5/8: Creating application user '${APP_USER}'..."
id "${APP_USER}" &>/dev/null || useradd -r -s /bin/false "${APP_USER}"
log "Application user created."

# --- Step 2: Install PostgreSQL 15 -------------------------------------------
log "Step 2/8: Installing PostgreSQL 15..."

# Add PostgreSQL official repository
if ! grep -q "apt.postgresql.org" /etc/apt/sources.list.d/*.list 2>/dev/null; then
    sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
    wget -qO- https://www.postgresql.org/media/keys/ACCC4CF8.asc | gpg --dearmor -o /etc/apt/trusted.gpg.d/postgresql.gpg 2>/dev/null || \
    wget -qO- https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add - 2>/dev/null
    apt-get update -qq
fi

apt-get install -y -qq postgresql-15 postgresql-contrib-15

# Start PostgreSQL
systemctl enable postgresql
systemctl start postgresql

# Configure PostgreSQL authentication
PG_HBA=$(sudo -u postgres psql -t -c "SHOW hba_file;" 2>/dev/null | xargs)
if [[ -n "$PG_HBA" ]] && ! grep -q "performance_portal" "$PG_HBA" 2>/dev/null; then
    # Change peer to md5 for local connections
    sed -i 's/^local\s\+all\s\+all\s\+peer/local   all             all                                     md5/' "$PG_HBA"
    systemctl restart postgresql
fi

# Create database
sudo -u postgres psql -c "ALTER USER postgres PASSWORD '${POSTGRES_PASSWORD}';" 2>/dev/null || true
sudo -u postgres psql -c "CREATE DATABASE ${POSTGRES_DB};" 2>/dev/null || true
log "PostgreSQL 15 installed and configured."

# --- Step 3: Install Java 17 -------------------------------------------------
log "Step 3/8: Installing Java 17 (Temurin)..."
apt-get install -y -qq openjdk-17-jdk openjdk-17-jre
java -version 2>&1 | head -1
log "Java 17 installed."

# --- Step 4: Install Maven ---------------------------------------------------
log "Step 4/8: Installing Maven..."
apt-get install -y -qq maven
mvn --version 2>&1 | head -1
log "Maven installed."

# --- Step 5: Install Node.js 18 & Nginx --------------------------------------
log "Step 5/8: Installing Node.js 18 and Nginx..."

# Node.js 18 LTS via NodeSource
if ! command -v node &>/dev/null || [[ "$(node -v 2>/dev/null)" != v18* ]]; then
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash - 2>/dev/null
    apt-get install -y -qq nodejs
fi

# Nginx
apt-get install -y -qq nginx
log "Node.js $(node -v) and Nginx installed."

# --- Step 6: Build & Deploy Application --------------------------------------
log "Step 6/8: Building application..."

# Create directories
mkdir -p "${APP_DIR}" "${DATA_DIR}"/{uploads,diagrams,reports,templates}
mkdir -p "${REPO_DIR}"

# Set proper ownership
chown -R "${APP_USER}:${APP_USER}" "${APP_DIR}"
chown -R "${APP_USER}:${APP_USER}" "${DATA_DIR}"

# Copy source code
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

if [[ -d "${SOURCE_ROOT}/backend" ]]; then
    log "Copying source code from ${SOURCE_ROOT}..."
    cp -r "${SOURCE_ROOT}/backend" "${REPO_DIR}/"
    cp -r "${SOURCE_ROOT}/frontend" "${REPO_DIR}/"
    cp -r "${SOURCE_ROOT}/db-init" "${REPO_DIR}/" 2>/dev/null || true
else
    error "Source code not found. Place this script in deploy/ubuntu/ within the project."
fi

# Build backend
log "Building backend JAR..."
cd "${REPO_DIR}/backend"
mvn clean package -DskipTests -B -q
cp target/performance-portal-backend.jar "${APP_DIR}/app.jar"
log "Backend JAR built."

# Build frontend
log "Building frontend..."
cd "${REPO_DIR}/frontend"
npm install --silent 2>/dev/null
npm run build 2>/dev/null
mkdir -p /var/www/perf-portal
cp -r dist/* /var/www/perf-portal/
log "Frontend built."

# --- Step 7: Configure Services -----------------------------------------------
log "Step 7/8: Configuring services..."

# Create systemd service for backend
cat > /etc/systemd/system/perf-portal.service <<EOF
[Unit]
Description=Performance Portal Backend
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=${APP_USER}
Group=${APP_USER}
WorkingDirectory=${APP_DIR}
Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/${POSTGRES_DB}"
Environment="SPRING_DATASOURCE_USERNAME=${POSTGRES_USER}"
Environment="SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}"
Environment="UPLOAD_DIR=${DATA_DIR}/uploads"
Environment="DIAGRAMS_DIR=${DATA_DIR}/diagrams"
Environment="REPORTS_DIR=${DATA_DIR}/reports"
Environment="TEMPLATE_DIR=${DATA_DIR}/templates"
Environment="JAVA_OPTS=${JAVA_OPTS}"
ExecStart=/usr/bin/java ${JAVA_OPTS} -jar ${APP_DIR}/app.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=perf-portal

[Install]
WantedBy=multi-user.target
EOF

# Configure Nginx as reverse proxy
cat > /etc/nginx/sites-available/perf-portal <<'EOF'
server {
    listen 3000;
    server_name _;
    client_max_body_size 200M;

    root /var/www/perf-portal;
    index index.html;

    # Proxy API requests to backend
    location /api/v1/ {
        proxy_pass http://127.0.0.1:8080/api/v1/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_connect_timeout 300s;
        proxy_send_timeout 300s;
    }

    # SPA fallback
    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF

# Enable site, disable default
ln -sf /etc/nginx/sites-available/perf-portal /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default 2>/dev/null || true
nginx -t 2>/dev/null

# --- Step 8: Start Services & Firewall ----------------------------------------
log "Step 8/8: Starting services..."

systemctl daemon-reload
systemctl enable --now perf-portal
systemctl restart nginx
systemctl enable nginx

# UFW firewall rules
if command -v ufw &>/dev/null; then
    ufw allow ${FRONTEND_PORT}/tcp 2>/dev/null || true
    ufw allow ${BACKEND_PORT}/tcp 2>/dev/null || true
    ufw allow 22/tcp 2>/dev/null || true
    ufw --force enable 2>/dev/null || true
fi

# --- Health Check -------------------------------------------------------------
log "Waiting for backend to start (up to 60s)..."
for i in $(seq 1 12); do
    if curl -sf http://localhost:${BACKEND_PORT}/api/v1/capabilities >/dev/null 2>&1; then
        log "Backend is healthy!"
        break
    fi
    sleep 5
done

if ! curl -sf http://localhost:${BACKEND_PORT}/api/v1/capabilities >/dev/null 2>&1; then
    warn "Backend may still be starting. Check: journalctl -u perf-portal -f"
fi

# --- Save Credentials --------------------------------------------------------
log "Saving credentials to secure file..."
CREDS_FILE="/etc/performance-portal/.credentials"
mkdir -p /etc/performance-portal
cat > "${CREDS_FILE}" <<EOF
# Performance Portal Credentials
# Generated: $(date)
# KEEP THIS FILE SECURE - Contains database password

PostgreSQL Database: ${POSTGRES_DB}
PostgreSQL User: ${POSTGRES_USER}
PostgreSQL Password: ${POSTGRES_PASSWORD}
PostgreSQL Connection: postgresql://localhost:5432/${POSTGRES_DB}

Application User: ${APP_USER}
Application Directory: ${APP_DIR}
Data Directory: ${DATA_DIR}
EOF
chmod 600 "${CREDS_FILE}"
log "Credentials saved to ${CREDS_FILE} with restricted permissions (600)."

# --- Done ---------------------------------------------------------------------
echo ""
echo "============================================================================="
echo -e "${GREEN}  Performance Portal deployed successfully on Ubuntu 22.04!${NC}"
echo "============================================================================="
echo ""
echo "  Frontend:  http://<server-ip>:${FRONTEND_PORT}"
echo "  Backend:   http://<server-ip>:${BACKEND_PORT}/api/v1"
echo "  Database:  PostgreSQL 15 on localhost:5432/${POSTGRES_DB}"
echo ""
echo "  Manage services:"
echo "    sudo systemctl status perf-portal    # Backend status"
echo "    sudo systemctl status nginx          # Frontend status"
echo "    sudo journalctl -u perf-portal -f    # Backend logs"
echo ""
echo "  Data directory: ${DATA_DIR}"
echo "============================================================================="
