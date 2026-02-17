# durandhar Performance Portal - Backend

## Overview
Spring Boot backend application for the durandhar Performance Test Reporting Portal using Java Stack (Option C).

## Technology Stack
- **Framework**: Spring Boot 3.2.2
- **Language**: Java 17
- **Database**: PostgreSQL
- **Security**: Spring Security + JWT
- **File Processing**: Apache POI (Excel), Apache Commons CSV
- **Report Generation**: iText (PDF), Apache POI (Word)
- **Charts**: JFreeChart
- **Job Queue**: Spring Batch + RabbitMQ
- **Build Tool**: Maven

## Prerequisites
- Java 17 or higher
- Maven 3.8+
- PostgreSQL 14+
- RabbitMQ 3.12+ (optional, for async processing)

## Project Structure
```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/hamza/durandhar/performance/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── exception/       # Custom exceptions
│   │   │   ├── parser/          # File parsers (JTL, CSV, Excel)
│   │   │   ├── repository/      # JPA repositories
│   │   │   ├── security/        # Security components
│   │   │   └── service/         # Business logic services
│   │   └── resources/
│   │       ├── application.yml  # Application configuration
│   │       └── schema.sql       # Database schema
│   └── test/                    # Unit and integration tests
├── pom.xml                      # Maven dependencies
└── README.md
```

## Database Setup

### 1. Create PostgreSQL Database
```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE performance_portal;

# Create user (optional)
CREATE USER perf_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE performance_portal TO perf_user;
```

### 2. Run Schema Script
```bash
psql -U postgres -d performance_portal -f src/main/resources/schema.sql
```

## Configuration

### Application Properties
Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/performance_portal
    username: postgres
    password: your_password

app:
  jwt:
    secret: your-256-bit-secret-key-change-this-in-production
  file:
    upload-dir: ./uploads
  report:
    output-dir: ./reports
```

### Environment Variables (Production)
```bash
export JWT_SECRET=your-production-secret-key
export DB_URL=jdbc:postgresql://prod-host:5432/performance_portal
export DB_USERNAME=prod_user
export DB_PASSWORD=prod_password
```

## Building the Application

### Maven Build
```bash
# Clean and build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run tests only
mvn test
```

## Running the Application

### Development Mode
```bash
# Using Maven
mvn spring-boot:run

# Using Java
java -jar target/performance-portal-1.0.0.jar
```

### Production Mode
```bash
java -jar target/performance-portal-1.0.0.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

## Docker Compose Deployment

From the repo root:

```bash
./deploy/docker/deploy.sh
```

To stop the stack:

```bash
./deploy/docker/undeploy.sh
```

## API Endpoints

### Authentication
- `POST /auth/login` - User login
- `POST /auth/register` - User registration
- `POST /auth/refresh` - Refresh token

### File Upload
- `POST /upload` - Upload test file
- `GET /upload/status/{id}` - Check upload status

### Test Runs
- `GET /tests` - List all test runs
- `GET /tests/{id}` - Get test run details
- `DELETE /tests/{id}` - Delete test run

### Capabilities
- `GET /capabilities` - List all capabilities
- `POST /capabilities` - Create capability (Admin only)

### Reports
- `POST /reports/generate` - Generate report
- `GET /reports/{id}` - Download report
- `GET /reports/list` - List all reports

### Analytics
- `GET /analytics/summary` - Get performance summary
- `GET /analytics/trends` - Get historical trends

## Default Credentials
```
Username: admin
Password: admin123
```
**⚠️ Change these credentials immediately in production!**

## File Upload Limits
- Maximum file size: 100MB
- Allowed formats: .jtl, .csv, .xlsx, .xls
- Upload directory: `./uploads`

## Report Generation
- Output directory: `./reports`
- Supported formats: PDF, HTML, Word, CSV
- Template directory: `./templates`

## Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=TestClassName
```

### Integration Tests
```bash
mvn verify
```

## Logging
Logs are stored in `logs/application.log`

### Log Levels
- Production: INFO
- Development: DEBUG
- Test: WARN

## Performance Optimization
- Connection pooling: HikariCP (default)
- Batch insert size: 20
- Query optimization with indexes
- Async processing for large files

## Security Features
- JWT-based authentication
- BCrypt password hashing
- Role-based access control (RBAC)
- CORS configuration
- Rate limiting (TODO)
- File validation and virus scanning (TODO)

## Monitoring
Access actuator endpoints:
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Info: `http://localhost:8080/actuator/info`

## Troubleshooting

### Database Connection Issues
```bash
# Check PostgreSQL is running
sudo systemctl status postgresql

# Test connection
psql -U postgres -d performance_portal -c "SELECT 1;"
```

### Port Already in Use
```bash
# Change port in application.yml
server:
  port: 8081
```

### Out of Memory
```bash
# Increase heap size
java -Xmx2g -jar target/performance-portal-1.0.0.jar
```

## Development Guidelines
1. Follow Java naming conventions
2. Write unit tests for all services
3. Document all public APIs
4. Use Lombok to reduce boilerplate
5. Handle exceptions properly
6. Log important events

## Contributing
1. Create feature branch
2. Write tests
3. Update documentation
4. Submit pull request

## License
Hamza Internal Use Only

## Support
For issues and questions, contact the development team.

## Version History
- v1.0.0 (2026-02-10) - Initial release
  - Basic authentication
  - File upload and parsing
  - Database schema
  - Security configuration