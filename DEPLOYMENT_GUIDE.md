# Production Deployment Guide

## Prerequisites

1. **GitHub Account** with repository access
2. **Production Server** (Linux, minimum 4GB RAM, 20GB disk)
3. **Domain Name** (optional, for HTTPS)
4. **Docker & Docker Compose** installed on production server

---

## Step 1: Push Code to GitHub

### Create GitHub Repository

```bash
# Initialize git (if not already done)
cd /path/to/performance-portal
git init

# Add GitHub remote
git remote add origin https://github.com/your-org/performance-portal.git

# Create .gitignore
cat > .gitignore << 'EOF'
# Environment files
.env
.env.local
.env.production
*.env

# Build outputs
backend/target/
frontend/dist/
frontend/node_modules/
backend/logs/

# IDE
.idea/
.vscode/
*.iml

# OS
.DS_Store
Thumbs.db

# Docker volumes
postgres_data/
app_data/

# Sensitive
*.key
*.pem
.htpasswd
EOF

# Add and commit
git add .
git commit -m "Initial commit with security hardening"
git branch -M main
git push -u origin main
```

---

## Step 2: Configure GitHub Secrets

Go to **Settings → Secrets and variables → Actions** in your GitHub repository:

### Required Secrets

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `PRODUCTION_HOST` | Your server IP/hostname | `203.0.113.10` |
| `PRODUCTION_USER` | SSH username | `deploy` |
| `SSH_PRIVATE_KEY` | SSH private key for deployment | `-----BEGIN RSA PRIVATE KEY-----...` |
| `SSH_PORT` | SSH port (optional) | `22` |
| `DB_PASSWORD` | PostgreSQL password | Strong random password |
| `APP_USERNAME` | Application username | `admin` |
| `APP_PASSWORD` | Application password | Strong 32+ char password |
| `JWT_SECRET` | JWT signing secret | 256-bit base64 string |

### Generate Strong Secrets

```bash
# Generate PostgreSQL password
openssl rand -base64 32

# Generate application password
openssl rand -base64 48

# Generate JWT secret (256-bit)
openssl rand -base64 64
```

---

## Step 3: Server Setup

### Install Docker & Docker Compose

```bash
# SSH into your server
ssh user@your-server-ip

# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo apt install docker-compose-plugin -y

# Verify installation
docker --version
docker compose version
```

### Create Deployment Directory

```bash
# Create deployment directory
sudo mkdir -p /opt/performance-portal
sudo chown $USER:$USER /opt/performance-portal
cd /opt/performance-portal

# Clone repository (or get docker-compose files)
git clone https://github.com/your-org/performance-portal.git .
```

### Configure Environment

```bash
# Copy environment template
cp .env.production.example .env.production

# Edit with production values
nano .env.production
```

Fill in the values:
```env
GITHUB_REPOSITORY=your-org/performance-portal
IMAGE_TAG=main
DB_USER=perfportal_user
DB_PASSWORD=<STRONG_PASSWORD_FROM_GITHUB_SECRETS>
APP_USERNAME=admin
APP_PASSWORD=<STRONG_PASSWORD_FROM_GITHUB_SECRETS>
JWT_SECRET=<JWT_SECRET_FROM_GITHUB_SECRETS>
DDL_AUTO=validate
CORS_ORIGINS=https://perf.yourdomain.com
SPRING_PROFILE=production
```

### Configure GitHub Container Registry Access

```bash
# Create GitHub Personal Access Token (PAT) with read:packages scope
# Then login to GitHub Container Registry
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
```

---

## Step 4: Initial Deployment

### First-Time Setup (Schema Creation)

```bash
cd /opt/performance-portal

# Set DDL_AUTO to update for initial schema creation
export DDL_AUTO=update

# Pull images
docker compose -f docker-compose.prod.yml pull

# Start services
docker compose -f docker-compose.prod.yml up -d

# Check logs
docker compose -f docker-compose.prod.yml logs -f backend

# Wait for "Started PerformancePortalApplication" message
# Then Ctrl+C to exit logs
```

### Verify Deployment

```bash
# Check container status
docker compose -f docker-compose.prod.yml ps

# Test backend (should return 401)
curl -I http://localhost:8080/api/v1/capabilities

# Test backend with auth (should return 200)
curl -u admin:YOUR_PASSWORD http://localhost:8080/api/v1/capabilities

# Test frontend (should return 401)
curl -I http://localhost

# All services healthy? Update to validate mode
nano .env.production  # Change DDL_AUTO=validate

# Restart backend
docker compose -f docker-compose.prod.yml restart backend
```

---

## Step 5: Configure Reverse Proxy (Optional but Recommended)

### Install Nginx

```bash
sudo apt install nginx -y
```

### Configure Nginx

```bash
sudo nano /etc/nginx/sites-available/performance-portal
```

Add:
```nginx
server {
    listen 80;
    server_name perf.yourdomain.com;

    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name perf.yourdomain.com;

    # SSL certificates (obtain via Let's Encrypt - see below)
    ssl_certificate /etc/letsencrypt/live/perf.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/perf.yourdomain.com/privkey.pem;

    # SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;

    # Proxy to frontend
    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Large file uploads
    client_max_body_size 200M;
}
```

Enable site:
```bash
sudo ln -s /etc/nginx/sites-available/performance-portal /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### Obtain SSL Certificate (Let's Encrypt)

```bash
# Install certbot
sudo apt install certbot python3-certbot-nginx -y

# Obtain certificate
sudo certbot --nginx -d perf.yourdomain.com

# Certbot will automatically configure nginx
# Certificates auto-renew via cron
```

---

## Step 6: Configure Firewall

```bash
# Allow SSH
sudo ufw allow ssh

# Allow HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Enable firewall
sudo ufw enable
sudo ufw status
```

---

## Step 7: Automated Deployment via GitHub Actions

Once GitHub Actions are configured, deployments happen automatically:

### Deployment Triggers

1. **Push to `main` branch** → Automatic deployment to production
2. **Create version tag** (e.g., `v1.0.0`) → Tagged release deployment
3. **Manual trigger** → Workflow dispatch from GitHub Actions UI

### Monitor Deployment

```bash
# View GitHub Actions: Repository → Actions tab
# Or via CLI:
gh workflow view "Deploy to Production"

# Check deployment logs on server:
docker compose -f docker-compose.prod.yml logs -f
```

---

## Step 8: Monitoring & Maintenance

### View Logs

```bash
# All services
docker compose -f docker-compose.prod.yml logs -f

# Specific service
docker compose -f docker-compose.prod.yml logs -f backend

# Last 100 lines
docker compose -f docker-compose.prod.yml logs --tail=100 backend
```

### Backup Database

```bash
# Create backup script
cat > /opt/performance-portal/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/backups/performance-portal"
mkdir -p $BACKUP_DIR
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Backup database
docker exec perf-portal-db pg_dump -U perfportal_user performance_portal | \
  gzip > $BACKUP_DIR/db_backup_$TIMESTAMP.sql.gz

# Backup uploaded files
tar -czf $BACKUP_DIR/uploads_$TIMESTAMP.tar.gz /var/lib/docker/volumes/performance-portal_app_data

# Keep only last 7 days
find $BACKUP_DIR -name "db_backup_*.sql.gz" -mtime +7 -delete
find $BACKUP_DIR -name "uploads_*.tar.gz" -mtime +7 -delete

echo "Backup completed: $TIMESTAMP"
EOF

chmod +x /opt/performance-portal/backup.sh

# Add to crontab (daily at 2 AM)
(crontab -l 2>/dev/null; echo "0 2 * * * /opt/performance-portal/backup.sh") | crontab -
```

### Update Application

```bash
cd /opt/performance-portal

# Pull latest images
docker compose -f docker-compose.prod.yml pull

# Restart services
docker compose -f docker-compose.prod.yml up -d

# Check status
docker compose -f docker-compose.prod.yml ps
```

### Rollback

```bash
# Set specific version
export IMAGE_TAG=v1.0.0

# Pull and restart
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

---

## Alternative Deployment Options

### Option 1: AWS ECS/Fargate

Use GitHub Actions to deploy to AWS:
- Configure AWS credentials in GitHub Secrets
- Use `aws-actions/amazon-ecs-deploy-task-definition` action
- Store images in Amazon ECR

### Option 2: Kubernetes

Create Kubernetes manifests:
```bash
# Generate K8s manifests
kubectl create deployment backend --image=ghcr.io/org/backend:latest --dry-run=client -o yaml > k8s/backend-deployment.yaml
```

Use GitHub Actions with `azure/k8s-deploy` or `google-github-actions/get-gke-credentials`

### Option 3: Platform as a Service

- **Railway**: Connect GitHub repo, automatic deployments
- **Render**: Dockerfile-based deployment
- **Fly.io**: Simple CLI-based deployment
- **DigitalOcean App Platform**: GitHub integration

---

## Security Best Practices

1. ✅ **Never commit secrets** to repository
2. ✅ **Use GitHub Secrets** for sensitive data
3. ✅ **Enable 2FA** on GitHub account
4. ✅ **Restrict SSH access** (key-based only, disable password auth)
5. ✅ **Keep system updated**: `sudo apt update && sudo apt upgrade`
6. ✅ **Monitor logs** for suspicious activity
7. ✅ **Backup regularly** (automated daily backups)
8. ✅ **Use HTTPS** in production (Let's Encrypt)
9. ✅ **Limit container privileges** (non-root users)
10. ✅ **Scan images** for vulnerabilities (Trivy, Snyk)

---

## Troubleshooting

### Container won't start

```bash
# Check logs
docker compose -f docker-compose.prod.yml logs backend

# Check environment variables
docker compose -f docker-compose.prod.yml config

# Restart services
docker compose -f docker-compose.prod.yml restart
```

### Database connection issues

```bash
# Check database is running
docker compose -f docker-compose.prod.yml ps postgres

# Test connection
docker exec -it perf-portal-db psql -U perfportal_user -d performance_portal
```

### Authentication failures

```bash
# Verify credentials in .env.production
cat .env.production

# Check backend logs for auth errors
docker compose -f docker-compose.prod.yml logs backend | grep -i auth

# Regenerate htpasswd if needed
docker exec perf-portal-frontend htpasswd -cb /etc/nginx/.htpasswd admin newpassword
```

### High memory usage

```bash
# Check container stats
docker stats

# Adjust JVM memory in .env.production
JAVA_OPTS="-Xmx2g -Xms1g"

# Restart backend
docker compose -f docker-compose.prod.yml restart backend
```

---

## Support & Documentation

- **GitHub Issues**: Report bugs or request features
- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Docker Docs**: https://docs.docker.com/
- **Nginx Docs**: https://nginx.org/en/docs/

---

**Last Updated**: 2026-02-19
**Version**: 1.0.0
