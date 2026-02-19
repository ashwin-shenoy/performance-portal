# Performance Test Portal

Web application for managing and generating performance test reports for Performance Portal iPaaS.

## 🚀 Features

- **Performance Dashboard** - View all test results, metrics, and analytics
- **JMeter File Upload** - Upload and parse JTL files
- **Report Generation** - Professional Word/PDF reports with charts
- **Capability Management** - Track performance across different capabilities
- **Architecture Diagrams** - Upload and manage system diagrams
- **Baseline Tracking** - Compare against performance baselines

## 🔒 Security (Production Ready)

✅ **Authentication Required** - All endpoints protected  
✅ **Security Headers** - CSP, X-Frame-Options, HSTS  
✅ **File Upload Protection** - Path traversal prevention  
✅ **XXE Protection** - Hardened XML parser  
✅ **No Hardcoded Secrets** - Environment-based configuration  
✅ **Session Storage** - Secure token handling  

## 📦 Quick Start

### Local Development

```bash
# Start all services
docker compose up -d

# Access application
# Frontend: http://localhost:3000
# Backend: http://localhost:8080/api/v1
# Default credentials: admin / change-me-now
```

### Production Deployment

See **[QUICK_START.md](QUICK_START.md)** for 5-minute deployment guide or **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** for complete instructions.

**Quick Deploy with GitHub:**
1. Push code to GitHub repository
2. Configure GitHub Secrets (credentials, server info)
3. GitHub Actions automatically builds and deploys
4. Access via your domain with HTTPS

## 🛠️ Technology Stack

### Backend
- Java 17
- Spring Boot 3.2.2
- Spring Security (Basic Auth)
- PostgreSQL 15
- Hibernate JPA
- Apache POI (Excel/Word generation)
- JFreeChart (Charts/Graphs)

### Frontend
- React 18
- Vite
- Carbon Design System
- Axios
- React Router

### Infrastructure
- Docker & Docker Compose
- Nginx (reverse proxy)
- GitHub Actions (CI/CD)

## 📁 Project Structure

```
.
├── backend/              # Spring Boot API
│   ├── src/
│   │   └── main/
│   │       ├── java/    # Application code
│   │       └── resources/
│   ├── Dockerfile
│   └── pom.xml
├── frontend/            # React SPA
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   └── services/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── .github/
│   └── workflows/       # CI/CD pipelines
├── docker-compose.yml   # Local development
├── docker-compose.prod.yml  # Production
└── docs/               # Documentation
```

## 🔧 Configuration

### Environment Variables

Required for production (set in `.env.production` or GitHub Secrets):

```env
# Database
DB_USER=perfportal
DB_PASSWORD=<strong-password>

# Security
APP_USERNAME=admin
APP_PASSWORD=<strong-32-char-password>
JWT_SECRET=<256-bit-base64-string>

# CORS
CORS_ORIGINS=https://your-domain.com

# JPA
DDL_AUTO=validate  # or 'update' for first deployment
```

Generate strong secrets:
```bash
openssl rand -base64 32  # Passwords
openssl rand -base64 64  # JWT secret
```

## 📚 Documentation

- **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** - Complete production deployment instructions
- **[QUICK_START.md](QUICK_START.md)** - 5-minute deployment guide
- **[SECURITY_HARDENING_SUMMARY.md](SECURITY_HARDENING_SUMMARY.md)** - Security improvements documentation
- **[docs/capability-onboarding.md](docs/capability-onboarding.md)** - Adding new capabilities
- **[docs/test-capability-api-guide.md](docs/test-capability-api-guide.md)** - API reference

## 🚦 CI/CD Pipeline

Automated workflows:

- **Build & Test** - Runs on every push/PR
  - Backend Maven build + tests
  - Frontend npm build
  - Docker image builds

- **Deploy to Production** - Runs on push to `main`
  - Build Docker images
  - Push to GitHub Container Registry
  - SSH deploy to production server
  - Automatic rollback on failure

## 🔐 Default Credentials

**⚠️ IMPORTANT:** Change these immediately in production!

- **Username:** `admin`
- **Password:** `change-me-now`

Update in:
- `docker-compose.yml` (development)
- `.env.production` (production)
- GitHub Secrets (CI/CD)

## 🧪 Testing

### Backend Tests
```bash
cd backend
mvn test
```

### Frontend Tests
```bash
cd frontend
npm test
```

### Integration Tests
```bash
# Start services
docker compose up -d

# Run API tests
curl -u admin:change-me-now http://localhost:8080/api/v1/capabilities
```

## 📊 Monitoring

### View Logs
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f backend
```

### Health Checks
- Backend: `GET /api/v1/actuator/health`
- Database: Container healthcheck
- Frontend: Nginx status

## 🔄 Updates

### Development
```bash
git pull
docker compose up -d --build
```

### Production (via GitHub Actions)
```bash
git push origin main  # Automatic deployment
```

### Manual Production Update
```bash
cd /opt/performance-portal
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

## 🐛 Troubleshooting

### Common Issues

**Container won't start:**
```bash
docker compose logs backend
docker compose restart backend
```

**Authentication fails:**
- Check credentials in `.env` file
- Verify `APP_USERNAME` and `APP_PASSWORD`
- Regenerate htpasswd: `docker exec frontend htpasswd -cb /etc/nginx/.htpasswd admin newpass`

**Database connection issues:**
```bash
docker compose ps postgres
docker compose logs postgres
```

**Port conflicts:**
```bash
# Check what's using the port
lsof -i :8080
lsof -i :3000

# Change ports in docker-compose.yml
```

## 📝 License

Proprietary - Internal Use Only

## 🤝 Contributing

1. Create feature branch
2. Make changes with tests
3. Push and create Pull Request
4. CI/CD runs automatically
5. Merge after approval

## 📞 Support

- **GitHub Issues**: Report bugs or request features
- **Documentation**: See `docs/` directory
- **Security Issues**: Contact security team directly

## 🎯 Roadmap

- [ ] JWT authentication (token-based)
- [ ] User management UI
- [ ] Real-time monitoring dashboard
- [ ] Email notifications
- [ ] Advanced analytics
- [ ] API rate limiting
- [ ] Multi-tenant support

---

**Last Updated**: 2026-02-19  
**Version**: 1.0.0  
**Status**: ✅ Production Ready
