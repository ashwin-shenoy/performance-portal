#!/bin/bash
# =============================================================================
# Performance Portal - RHEL 9.6 Deployment Script (From Scratch)
# =============================================================================
# This script installs all prerequisites and deploys the Performance Portal
# on a fresh Red Hat Enterprise Linux 9.6 server.
#
# Usage:
#   chmod +x deploy.sh
#   sudo ./deploy.sh
#
# Prerequisites: Fresh RHEL 9.6 with internet access and root/sudo privileges
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

log "Starting Performance Portal deployment on RHEL 9.6..."

# --- Step 1: System Update ----------------------------------------------------
log "Step 1/8: Updating system packages..."
dnf update -y -q
dnf install -y -q curl wget git unzip tar firewalld

# --- Step 1.5: Create Application User ----------------------------------------
log "Step 1.5/8: Creating application user '${APP_USER}'..."
id "${APP_USER}" &>/dev/null || useradd -r -s /bin/false "${APP_USER}"
log "Application user created."

# --- Step 2: Install PostgreSQL 15 -------------------------------------------
log "Step 2/8: Installing PostgreSQL 15..."

# Install PostgreSQL repo
dnf install -y -q https://download.postgresql.org/pub/repos/yum/reporpms/EL-9-x86_64/pgdg-redhat-repo-latest.noarch.rpm 2>/dev/null || true

# Disable built-in PostgreSQL module
dnf -qy module disable postgresql 2>/dev/null || true

# Install PostgreSQL 15
dnf install -y -q postgresql15 postgresql15-server postgresql15-contrib

# Initialize and start PostgreSQL
/usr/pgsql-15/bin/postgresql-15-setup initdb 2>/dev/null || true
systemctl enable postgresql-15
systemctl start postgresql-15

# Configure PostgreSQL - allow local connections with password
PG_HBA="/var/lib/pgsql/15/data/pg_hba.conf"
if ! grep -q "performance_portal" "$PG_HBA" 2>/dev/null; then
    # Backup original
    cp "$PG_HBA" "${PG_HBA}.bak"
    # Replace default local auth with md5 for our database
    sed -i 's/^local\s\+all\s\+all\s\+peer/local   all             all                                     md5/' "$PG_HBA"
    sed -i 's/^host\s\+all\s\+all\s\+127.0.0.1\/32\s\+ident/host    all             all             127.0.0.1\/32            md5/' "$PG_HBA"
    sed -i 's/^host\s\+all\s\+all\s\+::1\/128\s\+ident/host    all             all             ::1\/128                 md5/' "$PG_HBA"
    systemctl restart postgresql-15
fi

# Create database and user
sudo -u postgres psql -c "ALTER USER postgres PASSWORD '${POSTGRES_PASSWORD}';" 2>/dev/null || true
sudo -u postgres psql -c "CREATE DATABASE ${POSTGRES_DB};" 2>/dev/null || true
log "PostgreSQL 15 installed and configured."

# --- Step 3: Install Java 17 -------------------------------------------------
log "Step 3/8: Installing Java 17 (Temurin)..."
dnf install -y -q java-17-openjdk java-17-openjdk-devel
java -version 2>&1 | head -1
log "Java 17 installed."

# --- Step 4: Install Maven ---------------------------------------------------
log "Step 4/8: Installing Maven..."
MAVEN_VERSION="3.9.6"
if ! command -v mvn &>/dev/null; then
    cd /tmp
    wget -q "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
    tar -xzf "apache-maven-${MAVEN_VERSION}-bin.tar.gz" -C /opt/
    ln -sf "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn
    rm -f "apache-maven-${MAVEN_VERSION}-bin.tar.gz"
fi
mvn --version 2>&1 | head -1
log "Maven installed."

# --- Step 5: Install Node.js 18 & Nginx --------------------------------------
log "Step 5/8: Installing Node.js 18 and Nginx..."
# Node.js 18 LTS
dnf module enable -y -q nodejs:18 2>/dev/null || true
dnf install -y -q nodejs npm

# Nginx
dnf install -y -q nginx
log "Node.js and Nginx installed."

# --- Step 6: Build & Deploy Application --------------------------------------
log "Step 6/8: Building application..."

# Create directories
mkdir -p "${APP_DIR}" "${DATA_DIR}"/{uploads,diagrams,reports,templates}
mkdir -p "${REPO_DIR}"

# Set proper ownership
chown -R "${APP_USER}:${APP_USER}" "${APP_DIR}"
chown -R "${APP_USER}:${APP_USER}" "${DATA_DIR}"

# Copy source code (assumes script is run from repo root or source is available)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

if [[ -d "${SOURCE_ROOT}/backend" ]]; then
    log "Copying source code from ${SOURCE_ROOT}..."
    cp -r "${SOURCE_ROOT}/backend" "${REPO_DIR}/"
    cp -r "${SOURCE_ROOT}/frontend" "${REPO_DIR}/"
    cp -r "${SOURCE_ROOT}/db-init" "${REPO_DIR}/" 2>/dev/null || true
else
    error "Source code not found. Place this script in deploy/rhel9/ within the project."
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
cp -r dist/* /usr/share/nginx/html/ 2>/dev/null || mkdir -p /usr/share/nginx/html && cp -r dist/* /usr/share/nginx/html/
log "Frontend built."

# --- Step 7: Configure Services -----------------------------------------------
log "Step 7/8: Configuring services..."

# Create systemd service for backend
cat > /etc/systemd/system/perf-portal.service <<EOF
[Unit]
Description=Performance Portal Backend
After=network.target postgresql-15.service
Requires=postgresql-15.service

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
cat > /etc/nginx/conf.d/perf-portal.conf <<'EOF'
server {
    listen 3000;
    server_name _;
    client_max_body_size 200M;

    root /usr/share/nginx/html;
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

# Remove default nginx config if it conflicts
rm -f /etc/nginx/conf.d/default.conf 2>/dev/null || true

# SELinux: allow Nginx to connect to backend
setsebool -P httpd_can_network_connect 1 2>/dev/null || true

# --- Step 8: Start Services & Firewall ----------------------------------------
log "Step 8/8: Starting services..."

systemctl daemon-reload
systemctl enable --now perf-portal
systemctl enable --now nginx

# Firewall rules
systemctl enable --now firewalld 2>/dev/null || true
firewall-cmd --permanent --add-port=${FRONTEND_PORT}/tcp 2>/dev/null || true
firewall-cmd --permanent --add-port=${BACKEND_PORT}/tcp 2>/dev/null || true
firewall-cmd --reload 2>/dev/null || true

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
echo -e "${GREEN}  Performance Portal deployed successfully on RHEL 9.6!${NC}"
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
