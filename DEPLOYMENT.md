# Word Search Game - Deployment Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Local Development](#local-development)
3. [Docker Deployment](#docker-deployment)
4. [Production Deployment](#production-deployment)
5. [Environment Variables](#environment-variables)
6. [Database Migrations](#database-migrations)
7. [Railway database (online leaderboards)](#railway-database-online-leaderboards)
8. [Monitoring & Health Checks](#monitoring--health-checks)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software
- **Java 21** (JDK 21 or higher)
- **Docker** 24.0+ and **Docker Compose** 2.20+
- **PostgreSQL** 16+ (for production)
- **Git** 2.40+

### Optional
- **Redis** 7+ (for caching)
- **Nginx** (for reverse proxy)
- **Kubernetes** (for container orchestration)

---

## Local Development

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/Word-search.git
cd Word-search/backend
```

### 2. Set Up Environment
```bash
# Copy example environment file
cp ../.env.example ../.env

# Edit .env with your local configuration
nano ../.env
```

### 3. Run with H2 Database (Development)
```bash
# Build the application
./gradlew build

# Run with development profile (uses H2 in-memory database)
./gradlew bootRun

# Or with environment variable
SPRING_PROFILES_ACTIVE=default ./gradlew bootRun
```

The application will be available at: `http://localhost:8080`

### 4. Run Tests
```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

---

## Docker Deployment

### 1. Using Docker Compose (Recommended)

```bash
# From the project root directory
cd /path/to/Word-search

# Create .env file
cp .env.example .env
# Edit .env with your configuration

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### 2. Build Docker Image Manually

```bash
cd backend

# Build the image
docker build -t wordsearch-backend:latest .

# Run the container
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/wordsearch \
  -e SPRING_DATASOURCE_USERNAME=wordsearch_user \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  -e JWT_SECRET=your_jwt_secret \
  --name wordsearch-backend \
  wordsearch-backend:latest
```

### 3. Docker Compose Services

The `docker-compose.yml` includes:
- **postgres**: PostgreSQL 16 database
- **redis**: Redis cache (optional)
- **backend**: Spring Boot application
- **nginx**: Reverse proxy (optional)

---

## Production Deployment

### Option 1: Cloud Platform (Railway, Render, Heroku)

#### Railway
```bash
# Install Railway CLI
npm install -g @railway/cli

# Login
railway login

# Create new project
railway init

# Deploy
railway up
```

#### Render
1. Connect your GitHub repository
2. Create a new **Web Service**
3. Set build command: `./gradlew build`
4. Set start command: `java -jar build/libs/*.jar`
5. Add environment variables from `.env.example`

### Option 2: AWS Deployment

#### Using EC2
```bash
# SSH into EC2 instance
ssh -i your-key.pem ec2-user@your-ec2-ip

# Install Docker
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user

# Clone and deploy
git clone https://github.com/yourusername/Word-search.git
cd Word-search
docker-compose up -d
```

#### Using ECS/Fargate
1. Push Docker image to ECR
2. Create ECS cluster
3. Define task definition
4. Create service with load balancer

### Option 3: Kubernetes Deployment

```bash
# Create namespace
kubectl create namespace wordsearch

# Create secrets
kubectl create secret generic wordsearch-secrets \
  --from-literal=postgres-password=your_password \
  --from-literal=jwt-secret=your_jwt_secret \
  -n wordsearch

# Apply configurations (you'll need to create these)
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/backend.yaml
kubectl apply -f k8s/ingress.yaml
```

---

## Environment Variables

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `production` |
| `SPRING_DATASOURCE_URL` | Database connection URL | `jdbc:postgresql://localhost:5432/wordsearch` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `wordsearch_user` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `secure_password_123` |
| `JWT_SECRET` | JWT signing secret (min 256 bits) | `your_secret_key_here` |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Application port | `8080` |
| `REDIS_HOST` | Redis hostname | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `*` |
| `LOG_LEVEL` | Logging level | `INFO` |

---

## Database Migrations

### Flyway Migrations

Migrations are automatically applied on application startup.

**Location**: `backend/src/main/resources/db/migration/`

**Migration Files**:
- `V1__Initial_Schema.sql` - Core tables
- `V2__Initial_Categories.sql` - Categories and word data
- `V3__Add_Achievements_And_Challenges.sql` - Achievements and daily challenges

### Manual Migration

```bash
# Run migrations manually
./gradlew flywayMigrate

# Check migration status
./gradlew flywayInfo

# Repair migrations (if needed)
./gradlew flywayRepair

# Rollback (create custom migration)
./gradlew flywayUndo
```

### Database Backup

```bash
# Backup PostgreSQL database
docker exec wordsearch-postgres pg_dump -U wordsearch_user wordsearch > backup.sql

# Restore database
docker exec -i wordsearch-postgres psql -U wordsearch_user wordsearch < backup.sql
```

---

## Railway database (online leaderboards)

The backend is plain Spring Boot + PostgreSQL. Railway hosts **both** the app (from
`backend/Dockerfile`, see `railway.json`/`railway.toml`) **and** a managed Postgres
database, so the competitive, league, and casual leaderboards are reachable online with
**no code changes** — only the `SPRING_DATASOURCE_*` environment variables.

### 1. Add a Postgres service
In your Railway project: **New → Database → Add PostgreSQL**. Railway provisions it and
exposes reference variables (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`).

### 2. Point the backend service at it (env vars)
On the **backend** service in Railway, set these variables (using Railway's reference
syntax so they track the Postgres service automatically):
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
SPRING_PROFILES_ACTIVE=production
JWT_SECRET=<a long random secret>
```
`application-production.yml` already reads these and uses `org.postgresql.Driver` — nothing
to edit in the app.

### 3. Deploy — migrations run automatically
On deploy, Flyway runs on boot (`spring.flyway.enabled=true`) and applies `V1..V6` to the
Railway Postgres. Verify the new tables exist (`leaderboard_entries`, `league_cohorts`,
`league_memberships`, `coin_transactions`) via Railway's Postgres "Data" tab or `psql`.

> Connection-pool note: keep HikariCP `maximum-pool-size` (production default 20) at or
> below the Postgres plan's connection limit; lower it if you see pool exhaustion.

## Monitoring & Health Checks

### Health Endpoints

```bash
# Application health
curl http://localhost:8080/actuator/health

# Detailed health (requires authorization)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

### Docker Health Checks

```bash
# Check container health
docker ps

# View health check logs
docker inspect wordsearch-backend | grep -A 10 Health
```

### Monitoring with Prometheus + Grafana

```yaml
# Add to docker-compose.yml
prometheus:
  image: prom/prometheus:latest
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
  ports:
    - "9090:9090"

grafana:
  image: grafana/grafana:latest
  ports:
    - "3000:3000"
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
```

---

## Troubleshooting

### Common Issues

#### 1. Database Connection Failed
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check database logs
docker logs wordsearch-postgres

# Test connection
docker exec -it wordsearch-postgres psql -U wordsearch_user -d wordsearch
```

#### 2. Application Won't Start
```bash
# Check application logs
docker logs wordsearch-backend

# Check Java version
java -version  # Should be 21+

# Verify environment variables
docker exec wordsearch-backend env | grep SPRING
```

#### 3. Flyway Migration Errors
```bash
# Check migration status
docker exec wordsearch-backend ./gradlew flywayInfo

# Repair checksums
docker exec wordsearch-backend ./gradlew flywayRepair

# Baseline (for existing database)
docker exec wordsearch-backend ./gradlew flywayBaseline
```

#### 4. Out of Memory
```bash
# Increase JVM memory
docker run -e JAVA_OPTS="-Xmx2g -Xms1g" ...

# Or in docker-compose.yml
environment:
  - JAVA_OPTS=-Xmx2g -Xms1g
```

#### 5. Slow Performance
```bash
# Check database connections
docker exec wordsearch-postgres psql -U wordsearch_user -d wordsearch \
  -c "SELECT count(*) FROM pg_stat_activity;"

# Check Redis connection
docker exec wordsearch-redis redis-cli ping

# View JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

### Debug Mode

```bash
# Enable debug logging
SPRING_PROFILES_ACTIVE=production \
LOGGING_LEVEL_COM_WORDSEARCH=DEBUG \
./gradlew bootRun

# Or in application.yml
logging:
  level:
    com.wordsearch: DEBUG
```

### Performance Tuning

```yaml
# application-production.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
```

---

## Security Checklist

- [ ] Change default JWT secret
- [ ] Use strong database passwords
- [ ] Enable HTTPS/SSL in production
- [ ] Configure CORS properly
- [ ] Enable rate limiting
- [ ] Set up firewall rules
- [ ] Regular security updates
- [ ] Database backup strategy
- [ ] Monitoring and alerting
- [ ] Log rotation configured

---

## Useful Commands

```bash
# View all containers
docker-compose ps

# Restart specific service
docker-compose restart backend

# View resource usage
docker stats

# Clean up Docker resources
docker system prune -a

# Export environment variables
export $(cat .env | xargs)

# Generate new JWT secret
openssl rand -base64 64
```

---

## Support

For issues and questions:
- GitHub Issues: https://github.com/yourusername/Word-search/issues
- Documentation: See README.md
- API Docs: http://localhost:8080/swagger-ui.html (when running)

---

**Last Updated**: 2025-11-06
**Version**: 1.0.0
