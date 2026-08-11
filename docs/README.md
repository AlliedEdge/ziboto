# Ziboto Documentation

Welcome to Ziboto's comprehensive documentation!

---

## 📚 Documentation Index

### 🚀 Getting Started
- **[Main README](../README.md)** - Project overview and quick start
- **[Quick Start Testing](../QUICK_START_TESTING.md)** - Start load testing in 5 minutes

### 📋 Planning & Roadmap
- **[Project Status Analysis](../PROJECT_STATUS_ANALYSIS.md)** - Complete project status (65% complete)
- **[Future Development](../FUTURE.md)** - Roadmap for v1, v2, v3 and beyond
- **[Changelog](../CHANGELOG.md)** - Version history and changes

### 🧪 Testing
- **[Load Testing Guide](./LOAD_TESTING_GUIDE.md)** ⭐ **NEW!** - Complete guide for testing 100,000+ users
  - k6 (recommended)
  - JMeter (industry standard)
  - Gatling (high performance)
  - Locust, Artillery
  - Complete test scripts included

### 🏗️ Architecture
- **[High-Level Design](./architecture/)** - System architecture diagrams
- **[Backend README](../apps/backend/README.md)** - Backend architecture and setup
- **[Frontend README](../apps/frontend/README.md)** - Frontend architecture and setup

### 🔐 Security
- **[Backend Security](../apps/backend/docs/SECURITY.md)** - Security features and best practices

---

## 🎯 Quick Links by Role

### For Developers
- [Backend Setup](../apps/backend/README.md#getting-started)
- [Frontend Setup](../apps/frontend/README.md#getting-started)
- [API Documentation](http://localhost:8080/swagger-ui.html) (when running)
- [Database Migrations](../apps/backend/src/main/resources/db/migration/)

### For DevOps/SRE
- [Load Testing Guide](./LOAD_TESTING_GUIDE.md)
- [Docker Deployment](../infra/docker/)
- [Nginx Configuration](../infra/nginx/)
- [Kubernetes Manifests](../infra/kubernetes/) (v3)

### For Project Managers
- [Project Status](../PROJECT_STATUS_ANALYSIS.md)
- [Roadmap](../FUTURE.md)
- [Changelog](../CHANGELOG.md)

### For QA/Testers
- [Load Testing Guide](./LOAD_TESTING_GUIDE.md)
- [Quick Start Testing](../QUICK_START_TESTING.md)
- [API Documentation](http://localhost:8080/swagger-ui.html)

---

## 📖 Documentation by Topic

### Authentication & Security
- JWT authentication with 15-min access tokens
- Refresh token mechanism (7-day expiry)
- Rate limiting (Redis-based)
- Account lockout protection
- Security headers (CSP, HSTS, X-Frame-Options)
- BCrypt password hashing

### File & Storage
- File upload/download with streaming
- Hierarchical folder structure
- Local storage (v1) / AWS S3 (planned)
- File metadata management
- Storage quota tracking

### Performance & Scalability
- Redis caching for sessions and rate limiting
- Database connection pooling
- Stateless architecture (horizontal scaling ready)
- Load testing for 100,000+ concurrent users

### Database
- PostgreSQL 15+ primary database
- Flyway migrations (10 migrations)
- Optimized indexes
- Connection pooling with HikariCP

### API Endpoints
- **Authentication:** 9 endpoints (login, register, refresh, etc.)
- **User Management:** 11 endpoints (profile, storage, admin)
- **File Management:** 6 endpoints (upload, download, list, search)
- **Folder Management:** 6 endpoints (create, rename, move, delete)

---

## 🔧 Technology Stack

### Backend
- Java 21
- Spring Boot 4.1.0
- PostgreSQL 15+
- Redis 7+
- JWT Authentication
- MapStruct, Lombok
- OpenAPI/Swagger

### Frontend
- React 19
- TypeScript 6
- Vite 8
- Tailwind CSS 4
- Zustand (state)
- Axios (HTTP)
- React Router DOM 7

### Infrastructure
- Docker + Docker Compose
- Nginx (reverse proxy)
- AWS S3 (planned)
- Kubernetes (v3)
- Terraform (v3)

---

## 📊 Project Metrics

- **Overall Completion:** ~65% (v1 MVP)
- **Code Lines:** ~15,000+ (backend + frontend)
- **API Endpoints:** 30+
- **Database Tables:** 7
- **Test Coverage:** ~10% (target: 60%+)
- **Documentation Pages:** 10+

---

## 🎯 Current Focus (v1 MVP)

**Top Priorities:**
1. ⭐ AWS S3 Integration
2. ⭐ Complete Frontend File Manager
3. ⭐ Load Testing (100k+ users)
4. ⭐ Docker Deployment
5. ⭐ Increase Test Coverage

**Target:** Complete v1 in 2-3 weeks

---

## 📞 Getting Help

### Documentation Not Clear?
- Check the specific module README:
  - [Backend](../apps/backend/README.md)
  - [Frontend](../apps/frontend/README.md)

### Need Code Examples?
- See test files in `apps/backend/src/test/`
- Check controller classes for API usage
- Review frontend service files

### Found an Issue?
- Check [Known Issues](../PROJECT_STATUS_ANALYSIS.md#known-issues--technical-debt)
- Review [Troubleshooting](#troubleshooting)

---

## 🐛 Troubleshooting

### Common Issues:

**Backend won't start:**
- Check PostgreSQL is running (port 5433)
- Check Redis is running (port 6380)
- Verify `.env` file has JWT_SECRET

**Frontend won't connect:**
- Check backend is running (port 8080)
- Verify VITE_API_URL in `.env`
- Check CORS configuration

**Database errors:**
- Run Flyway migrations: `./mvnw flyway:migrate`
- Check database credentials in `application.yml`

**Redis errors:**
- Verify Redis is running: `redis-cli ping`
- Check Redis connection in `application.yml`

**Load testing fails:**
- Start with small user count (100)
- Check connection pool sizes
- Monitor server resources

---

## 📝 Contributing to Documentation

### Adding New Documentation:

1. Create file in appropriate folder:
   - Architecture → `docs/architecture/`
   - API specs → `docs/api/`
   - Guides → `docs/`

2. Update this index (docs/README.md)

3. Follow naming convention:
   - Use UPPERCASE for major docs (README.md, CHANGELOG.md)
   - Use kebab-case for guides (load-testing-guide.md)

4. Include:
   - Clear title and purpose
   - Table of contents for long docs
   - Code examples where applicable
   - Last updated date

---

## 🗂️ File Structure

```
docs/
├── README.md                    # This file
├── LOAD_TESTING_GUIDE.md       # Complete load testing guide
├── architecture/                # Architecture diagrams and docs
│   ├── HLD/                    # High-level design
│   └── LLD/                    # Low-level design (planned)
└── api/                        # API specifications

../
├── README.md                   # Main project README
├── CHANGELOG.md                # Version history
├── FUTURE.md                   # Development roadmap
├── PROJECT_STATUS_ANALYSIS.md  # Current status
└── QUICK_START_TESTING.md      # Quick testing guide
```

---

## 🔗 External Resources

### Official Documentation:
- [Spring Boot](https://docs.spring.io/spring-boot/)
- [React](https://react.dev/)
- [PostgreSQL](https://www.postgresql.org/docs/)
- [Redis](https://redis.io/documentation)

### Load Testing:
- [k6](https://k6.io/docs/)
- [JMeter](https://jmeter.apache.org/usermanual/)
- [Gatling](https://gatling.io/docs/)

### Cloud:
- [AWS S3](https://docs.aws.amazon.com/s3/)
- [AWS EC2](https://docs.aws.amazon.com/ec2/)
- [Docker](https://docs.docker.com/)

---

## ✅ Documentation Checklist

When writing documentation:

- [ ] Clear title and purpose
- [ ] Target audience identified
- [ ] Prerequisites listed
- [ ] Step-by-step instructions
- [ ] Code examples included
- [ ] Expected outcomes described
- [ ] Troubleshooting section
- [ ] Last updated date
- [ ] Links to related docs

---

**Last Updated:** August 11, 2026  
**Maintainer:** Ziboto Team

---

*For questions or issues with documentation, please open an issue on GitHub.*
