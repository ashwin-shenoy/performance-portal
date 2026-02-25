# Quick Deployment Guide

## 🚀 Quick Start (5 Minutes)

### 1. Push to GitHub
```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_ORG/performance-portal.git
git push -u origin main
```

### 2. Configure GitHub Secrets
Go to **Settings → Secrets → Actions**, add:
- `PRODUCTION_HOST` - Your server IP
- `PRODUCTION_USER` - SSH username  
- `SSH_PRIVATE_KEY` - Your SSH private key
- `APP_PASSWORD` - Strong password (32+ chars)
- `DB_PASSWORD` - Database password
- `JWT_SECRET` - Run: `openssl rand -base64 64`

### 3. Setup Production Server
```bash
# Install Docker
curl -fsSL https://get.docker.com | sh

# Create deployment directory
sudo mkdir -p /opt/performance-portal
sudo chown $USER:$USER /opt/performance-portal
cd /opt/performance-portal

# Create environment file
cat > .env.production << 'EOF'
GITHUB_REPOSITORY=YOUR_ORG/performance-portal
IMAGE_TAG=main
DB_USER=perfportal
DB_PASSWORD=CHANGE_ME
APP_USERNAME=admin
APP_PASSWORD=CHANGE_ME
JWT_SECRET=CHANGE_ME
DDL_AUTO=update
CORS_ORIGINS=https://your-domain.com
EOF

# Login to GitHub Container Registry
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin

# Deploy
docker compose -f docker-compose.prod.yml up -d
```

### 4. Deploy from GitHub
- Push to `main` branch → Auto-deploys
- Or: Go to **Actions → Deploy to Production → Run workflow**

### 5. Verify
```bash
# Check status
docker compose -f docker-compose.prod.yml ps

# Test
curl -u admin:YOUR_PASSWORD http://YOUR_SERVER/api/v1/capabilities
```

### 6. Secure Frontend Build (Only if building images locally)

If your frontend dependencies require private npm access (for example `@hamza/*` packages), use Docker BuildKit secrets so tokens are not stored in image layers.

```bash
cd /opt/performance-portal

# Create gitignored npmrc from template
cp frontend/.npmrc.example frontend/.npmrc

# Export token only for current shell/session
export NPM_TOKEN=your-read-only-token
export DOCKER_BUILDKIT=1

# Build with compose (frontend build uses secret-mounted npmrc)
docker compose up -d --build
```

Minimal `frontend/.npmrc`:

```ini
@hamza:registry=https://registry.npmjs.org/
//registry.npmjs.org/:_authToken=${NPM_TOKEN}
always-auth=true
```

## 📦 What Gets Deployed

1. **PostgreSQL** - Database on port 5432 (localhost only)
2. **Backend** - Spring Boot API on port 8080 (localhost only)
3. **Frontend** - Nginx serving React app on port 80

## 🔒 Security Features (Already Configured)

✅ All endpoints require authentication  
✅ Security headers (CSP, X-Frame-Options, etc.)  
✅ File upload sanitization  
✅ XXE protection  
✅ Secrets via environment variables  
✅ No hardcoded credentials  

## 📊 Monitoring

```bash
# View logs
docker compose -f docker-compose.prod.yml logs -f

# Container status
docker compose -f docker-compose.prod.yml ps

# Resource usage
docker stats
```

## 🔄 Updates

Push to GitHub `main` branch → Automatic deployment via GitHub Actions

## 🆘 Need Help?

See full guide: [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)

## 🎯 Architecture

```
Internet → Nginx (Frontend) → Spring Boot (Backend) → PostgreSQL
         (Port 80/443)        (Port 8080)            (Port 5432)
```

## ⚡ Alternative Options

- **Kubernetes**: Use included manifests
- **AWS**: ECS/Fargate deployment
- **Cloud Platform**: Railway, Render, Fly.io
- **Manual**: SSH + Docker Compose

---

**Production Ready**: ✅  
**Time to Deploy**: ~5 minutes  
**Prerequisites**: Docker, GitHub repo, Production server
