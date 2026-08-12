# Changelog - Docker Infrastructure

All notable changes to the Docker infrastructure are documented here.

## [0.3.0] - 2026-08-12

### Security
- Added `.gitignore` to protect `.env` file and runtime data
- Prevents accidental commit of secrets and credentials
- Excludes Docker volumes and runtime data

### Protected Files
- `.env` - Environment variables with secrets (PostgreSQL, Redis, RabbitMQ, AWS, JWT)
- `docker-compose.override.yml` - Local overrides

---

## [0.2.0] - 2026-08-05

### Added (V2)
- RabbitMQ service for async messaging
- Email service configuration
- Elasticsearch service for full-text search
- Volume persistence for all services

### Enhanced
- PostgreSQL with health checks
- Redis with persistence configuration
- Service dependency management

---

## [0.1.0] - Initial Setup

### Added (V1)
- Docker Compose orchestration
- PostgreSQL 15 database service
- Redis 7 cache service
- Backend Spring Boot service
- Nginx reverse proxy
- Network configuration
- Volume management
- Health checks

### Configuration
- Database: PostgreSQL on port 5433
- Cache: Redis on port 6380
- Backend: Spring Boot on port 8080
- Proxy: Nginx on port 80
