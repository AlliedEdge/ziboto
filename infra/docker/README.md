# Ziboto Docker Deployment

Complete Docker deployment configuration for Ziboto file storage platform.

## Prerequisites

- Docker Engine 24.0+
- Docker Compose 2.20+
- 4GB RAM minimum (8GB recommended)
- 20GB disk space

## Quick Start

### 1. Development Mode (with PgAdmin & RedisInsight)

```bash
# Navigate to docker directory
cd infra/docker

# Copy environment file
copy .env.example .env

# Edit .env and add your AWS credentials (for local dev only)
notepad .env

# Start all services including dev tools
docker compose --profile dev up -d

# View logs
docker compose logs -f backend
```

**Access Points:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api/v1
- Nginx Proxy: http://localhost
- PgAdmin: http://localhost:5050 (admin@ziboto.com / admin123)
- RedisInsight: http://localhost:5540
- PostgreSQL: localhost:5433
- Redis: localhost:6380

### 2. Production Mode

```bash
# Start core services only
docker compose up -d

# Check health
docker compose ps
docker compose logs -f
```

## Architecture

```
┌─────────────────────────────────────────────┐
│              Nginx (Port 80)                │
│         Reverse Proxy & Load Balancer       │
└─────────┬───────────────────────┬───────────┘
          │                       │
          ▼                       ▼
┌─────────────────┐    ┌─────────────────────┐
│  Frontend:80    │    │  Backend:8080       │
│  (React + Vite) │    │  (Spring Boot)      │
└─────────────────┘    └──────┬──────┬───────┘
                              │      │
                    ┌─────────┘      └──────────┐
                    ▼                           ▼
          ┌──────────────────┐      ┌───────────────┐
          │  PostgreSQL:5432 │      │  Redis:6379   │
          │  (Database)      │      │  (Cache)      │
          └──────────────────┘      └───────────────┘
                    │
                    ▼
          ┌──────────────────┐
          │  AWS S3          │
          │  (File Storage)  │
          └──────────────────┘
```

## Services

### Core Services

1. **nginx** - Reverse proxy
   - Port: 80
   - Routes `/api/*` to backend
   - Routes `/*` to frontend
   - Load balancing
   - Health checks

2. **backend** - Spring Boot API
   - Port: 8080
   - Java 21 + Spring Boot 4.1.0
   - Actuator: `/actuator/health`
   - Multi-stage build
   - Security: Non-root user

3. **frontend** - React Application
   - Port: 3000 (internal 80)
   - React 19 + Vite 8
   - Nginx served
   - Optimized builds

4. **postgres** - PostgreSQL Database
   - Port: 5433 (mapped from 5432)
   - Version: 17
   - Persistent volume: `postgres_data`
   - Auto migrations via Flyway

5. **redis** - Cache & Sessions
   - Port: 6380 (mapped from 6379)
   - Version: 7.4
   - Persistent AOF
   - Persistent volume: `redis_data`

### Development Tools (--profile dev)

6. **pgadmin** - Database UI
   - Port: 5050
   - Username: admin@ziboto.com
   - Password: admin123

7. **redisinsight** - Redis UI
   - Port: 5540

## Configuration

### Environment Variables

Create `infra/docker/.env` from `.env.example`:

```bash
# Required
JWT_SECRET=your-secure-jwt-secret
AWS_REGION=eu-north-1
AWS_S3_BUCKET=your-bucket-name
RESEND_API_KEY=your-resend-api-key

# For local development only
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key

# Optional
VITE_API_URL=http://localhost:8080/api/v1
POSTGRES_PASSWORD=ziboto123
```

### AWS Credentials

**Local Development:**
- Use AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY in `.env`
- Or configure AWS CLI: `aws configure`

**Production (V3):**
- Use IAM roles (EKS Pod Identity / IRSA)
- Never use long-lived access keys

## Commands

### Basic Operations

```bash
# Start services
docker compose up -d

# Start with dev tools
docker compose --profile dev up -d

# Stop services
docker compose down

# Stop and remove volumes (CAUTION: deletes data)
docker compose down -v

# View logs
docker compose logs -f
docker compose logs -f backend
docker compose logs -f frontend

# Restart a service
docker compose restart backend

# Rebuild a service
docker compose up -d --build backend

# Check health
docker compose ps
docker compose top
```

### Maintenance

```bash
# View container stats
docker stats

# Execute command in container
docker compose exec backend sh
docker compose exec postgres psql -U ziboto

# Database backup
docker compose exec postgres pg_dump -U ziboto ziboto > backup.sql

# Database restore
docker compose exec -T postgres psql -U ziboto ziboto < backup.sql

# View volumes
docker volume ls

# Inspect volume
docker volume inspect ziboto_postgres_data
```

### Debugging

```bash
# View full logs
docker compose logs --tail=100 -f backend

# Check backend health
curl http://localhost:8080/actuator/health

# Check frontend health
curl http://localhost:3000/health

# Check nginx health
curl http://localhost/health

# Test database connection
docker compose exec postgres pg_isready -U ziboto

# Test Redis connection
docker compose exec redis redis-cli ping

# Inspect backend environment
docker compose exec backend env
```

## Build Details

### Backend Dockerfile

**Stage 1: Build**
- Base: maven:3.9-eclipse-temurin-21
- Maven dependency caching
- Compile and package
- Skips tests for faster builds

**Stage 2: Runtime**
- Base: eclipse-temurin:21-jre-alpine
- Non-root user (spring:spring)
- Container-optimized JVM settings
- Health check via actuator
- 75% max RAM usage

### Frontend Dockerfile

**Stage 1: Build**
- Base: node:20-alpine
- npm ci for production dependencies
- Vite production build

**Stage 2: Runtime**
- Base: nginx:1.27-alpine
- Custom nginx config
- Gzip compression
- Security headers
- React Router support

## Networking

All services run on the `ziboto-network` bridge network.

**Service DNS:**
- Backend accessible at: `backend:8080`
- Frontend accessible at: `frontend:80`
- Postgres accessible at: `postgres:5432`
- Redis accessible at: `redis:6379`

## Volumes

**Persistent Data:**
- `postgres_data` - PostgreSQL database files
- `redis_data` - Redis AOF persistence
- `backend_logs` - Application logs

**Backup Volumes:**
```bash
# Backup all volumes
docker run --rm -v ziboto_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz /data

# Restore volume
docker run --rm -v ziboto_postgres_data:/data -v $(pwd):/backup alpine tar xzf /backup/postgres-backup.tar.gz -C /
```

## Health Checks

All services have health checks configured:

- **Backend**: `/actuator/health` (30s interval, 60s start period)
- **Frontend**: `/health` (30s interval, 10s start period)
- **Nginx**: `/health` (30s interval)
- **PostgreSQL**: `pg_isready` (10s interval)
- **Redis**: `redis-cli ping` (10s interval)

## Security Considerations

### Development
- ✅ Non-root containers
- ✅ Network isolation
- ✅ Health checks
- ⚠️ Default passwords (change in production)
- ⚠️ Exposed ports (restrict in production)

### Production (V3)
- Use IAM roles instead of access keys
- Change all default passwords
- Use Docker secrets for sensitive data
- Restrict actuator endpoints
- Use HTTPS/TLS (nginx SSL)
- Enable firewall rules
- Implement log aggregation
- Use container image scanning

## Performance Tuning

### Database
```yaml
environment:
  POSTGRES_SHARED_BUFFERS: 256MB
  POSTGRES_EFFECTIVE_CACHE_SIZE: 1GB
  POSTGRES_MAX_CONNECTIONS: 100
```

### Backend
```yaml
environment:
  DATABASE_POOL_SIZE: 20
  SPRING_THREADS_MAX: 200
```

### Redis
```yaml
command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
```

## Troubleshooting

### Backend won't start
```bash
# Check logs
docker compose logs backend

# Common issues:
# 1. Database not ready - wait for postgres health check
# 2. Redis not ready - wait for redis health check
# 3. AWS credentials missing - check .env file
```

### Frontend build fails
```bash
# Rebuild with no cache
docker compose build --no-cache frontend

# Check Node version
docker compose run --rm frontend node --version
```

### Database connection issues
```bash
# Check postgres is running
docker compose ps postgres

# Test connection
docker compose exec postgres psql -U ziboto -c "SELECT 1"

# Check backend can reach postgres
docker compose exec backend ping postgres
```

### Port conflicts
```bash
# Check ports in use
netstat -an | findstr "8080"
netstat -an | findstr "5433"

# Change ports in docker-compose.yml
ports:
  - "8081:8080"  # Use 8081 instead
```

## Production Deployment Checklist

- [ ] Change all default passwords
- [ ] Generate new JWT secret (64+ bytes)
- [ ] Configure AWS IAM roles (no access keys)
- [ ] Set SPRING_PROFILES_ACTIVE=prod
- [ ] Configure CORS for production domain
- [ ] Set up SSL/TLS certificates
- [ ] Configure log aggregation
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Configure automated backups
- [ ] Test disaster recovery
- [ ] Implement secrets management
- [ ] Configure firewall rules
- [ ] Set resource limits (CPU/memory)
- [ ] Enable Docker Swarm or Kubernetes (V3)

## Next Steps

After successful Docker deployment:

1. **V1 Completion:**
   - Test email verification flow
   - Run load tests with k6
   - Security penetration testing

2. **V2 Features:**
   - Add RabbitMQ container
   - Implement file sharing
   - Add Elasticsearch container

3. **V3 Cloud-Native:**
   - Migrate to Kubernetes (EKS)
   - Deploy with Helm charts
   - Add Prometheus & Grafana
   - Implement CI/CD pipeline

## Support

For issues or questions:
- Check logs: `docker compose logs -f`
- Review configuration: `docker compose config`
- Inspect services: `docker compose ps`
- Read application logs: `docker compose exec backend cat logs/ziboto.log`

---

**Status:** Production Ready ✅  
**Last Updated:** August 11, 2026  
**Version:** V1.0.0
