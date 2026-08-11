# 👋 START HERE - Complete Ziboto Guide

**Welcome to Ziboto!** This is your one-stop guide to understand everything about this project.

---

## 🎯 What is Ziboto?

**Ziboto** is a **production-grade cloud storage platform** (like Google Drive/Dropbox) built to demonstrate:
- ✅ Enterprise backend engineering
- ✅ Cloud-native architecture
- ✅ Scalability (100,000+ concurrent users)
- ✅ Modern full-stack development

**Current Status:** 65% complete (v1 MVP in progress)

---

## 📚 Essential Documents (Read in Order)

### 1️⃣ **[README.md](./README.md)** - Start Here (10 min read)
**What:** Project overview, features, tech stack  
**When to read:** First thing you do  
**Key info:** Architecture, roadmap, quick start

### 2️⃣ **[PROJECT_STATUS_ANALYSIS.md](./PROJECT_STATUS_ANALYSIS.md)** - Full Analysis (30 min read)
**What:** Complete project breakdown - what's done, what's pending  
**When to read:** After README, before coding  
**Key info:** 
- Feature completion status (65%)
- All modules analyzed
- Database schema
- API endpoints (30+)
- Tech stack deep dive

### 3️⃣ **[FUTURE.md](./FUTURE.md)** - Development Roadmap (20 min read)
**What:** What needs to be done next  
**When to read:** Before starting development  
**Key info:**
- v1 priorities (AWS S3, load testing, Docker)
- v2 features (RabbitMQ, file sharing)
- v3 features (Kubernetes, Terraform)
- Timeline estimates

### 4️⃣ **[QUICK_START_TESTING.md](./QUICK_START_TESTING.md)** - Load Testing (5 min read)
**What:** How to test for 100k+ users  
**When to read:** When ready to test performance  
**Key info:** k6, JMeter, Gatling options

### 5️⃣ **[docs/LOAD_TESTING_GUIDE.md](./docs/LOAD_TESTING_GUIDE.md)** - Complete Testing Guide (1 hour read)
**What:** Detailed load testing with all tools and scripts  
**When to read:** When implementing load testing  
**Key info:** Complete test scripts, configuration, examples

---

## ⚡ Quick Answers to Your Questions

### ❓ "What's done so far?"

**✅ Complete (100%):**
- User authentication (JWT)
- User registration & login
- Password reset flow
- User profile management
- Folder management (hierarchical)
- File upload/download (basic)
- Redis caching & rate limiting
- Database schema (10 migrations)
- Security features (rate limit, account lockout)
- Frontend auth pages (all 12 pages)
- API documentation (Swagger)

**🚧 In Progress:**
- AWS S3 integration (using local storage now)
- Frontend file manager (page created, not connected)
- Email verification (backend ready, needs testing)

**❌ Not Started:**
- Docker deployment
- Load testing for 100k users
- Comprehensive tests (only 10% coverage)

---

### ❓ "Do I need RabbitMQ?"

**Answer: NO, not for v1**

- **You have:** Redis ✅ (fast, in-memory, good for caching/sessions)
- **You need:** Redis for v1 ✅
- **Add RabbitMQ in v2** for background jobs (thumbnails, virus scan, emails)

**Redis vs RabbitMQ:**
- **Redis:** Fast cache, sessions, rate limiting (you need this)
- **RabbitMQ:** Reliable message queue, background jobs (add later)

---

### ❓ "How do I test for 100,000+ users?"

**Answer: Use load testing tools**

**3 Options:**

1. **k6** ⭐ **Recommended**
   - Modern, easy, JavaScript-based
   - Install: `choco install k6`
   - Run: `k6 run test.js`

2. **JMeter** ⭐ **Most Popular**
   - Industry standard, GUI-based
   - Download from Apache
   - Run: `jmeter -n -t test.jmx`

3. **Gatling**
   - High-performance, Scala-based
   - Beautiful reports

**See:** [QUICK_START_TESTING.md](./QUICK_START_TESTING.md) for 5-minute guide  
**See:** [docs/LOAD_TESTING_GUIDE.md](./docs/LOAD_TESTING_GUIDE.md) for complete guide

---

### ❓ "What should I do next?"

**Next 2-3 Weeks (Complete v1 MVP):**

**Week 1:**
1. AWS S3 Integration (2-3 days) ⭐ **PRIORITY #1**
2. Complete Frontend File Manager (3-4 days) ⭐ **PRIORITY #2**
3. Test Email Verification (1 day)

**Week 2:**
4. Docker Deployment (2-3 days) ⭐ **PRIORITY #3**
5. Load Testing for 100k users (3-4 days) ⭐ **PRIORITY #4**
6. Write Tests (60% coverage) (1 week)

**After 2-3 weeks:** v1 MVP complete! 🎉

---

## 🏗️ Project Structure (Quick Overview)

```
Ziboto/
├── apps/
│   ├── backend/              # Spring Boot (Java 21)
│   │   ├── src/
│   │   ├── pom.xml
│   │   └── README.md         ← Backend setup guide
│   │
│   └── frontend/             # React 19 + TypeScript
│       ├── src/
│       ├── package.json
│       └── README.md         ← Frontend setup guide
│
├── docs/
│   ├── README.md             ← Documentation index
│   ├── LOAD_TESTING_GUIDE.md ← Complete testing guide
│   └── architecture/         ← Diagrams
│
├── infra/
│   ├── docker/               # Docker configs
│   ├── nginx/                # Nginx configs
│   ├── kubernetes/           # K8s manifests (v3)
│   └── terraform/            # IaC (v3)
│
├── README.md                 ← Project overview ⭐ START
├── PROJECT_STATUS_ANALYSIS.md ← Complete analysis (65% done)
├── FUTURE.md                 ← What's next (roadmap)
├── QUICK_START_TESTING.md    ← Load testing quickstart
├── START_HERE.md             ← This file
└── CHANGELOG.md              ← Version history
```

---

## 🚀 Running the Project (5 Minutes)

### Prerequisites:
- Java 21
- Node.js 18+
- PostgreSQL 16+ (port 5433)
- Redis 7+ (port 6380)
- Maven 3.9+

### Backend:

```bash
cd apps/backend

# Start dependencies
docker-compose up -d

# Generate JWT secret
./scripts/generate-jwt-secret.ps1  # Windows
./scripts/generate-jwt-secret.sh   # Linux/Mac

# Update .env with JWT_SECRET

# Run
./mvnw spring-boot:run
```

**Backend runs on:** http://localhost:8080  
**API Docs:** http://localhost:8080/swagger-ui.html

### Frontend:

```bash
cd apps/frontend

# Install
npm install

# Create .env
cp .env.example .env

# Run
npm run dev
```

**Frontend runs on:** http://localhost:5173

---

## 📊 Current Stats

| Metric | Value |
|--------|-------|
| **Overall Completion** | 65% |
| **Code Lines** | ~15,000+ |
| **API Endpoints** | 30+ |
| **Database Tables** | 7 |
| **Database Migrations** | 10 |
| **Frontend Pages** | 12 |
| **Test Coverage** | ~10% (target: 60%+) |
| **Documentation Pages** | 10+ |

---

## 🎯 Feature Checklist

### ✅ Completed (v1)
- [x] User authentication (JWT)
- [x] User registration & login
- [x] Password reset flow
- [x] Refresh token mechanism
- [x] User profile management
- [x] Folder management (hierarchical)
- [x] File upload/download (basic)
- [x] Redis caching
- [x] Rate limiting
- [x] Security headers
- [x] Audit logging
- [x] Database migrations
- [x] API documentation
- [x] Frontend auth pages

### 🚧 In Progress (v1)
- [ ] AWS S3 integration ⭐
- [ ] Frontend file manager ⭐
- [ ] Email verification testing
- [ ] Docker deployment ⭐
- [ ] Load testing (100k users) ⭐
- [ ] Test coverage (60%+) ⭐

### ❌ Future (v2+)
- [ ] RabbitMQ (background jobs)
- [ ] File sharing (public links)
- [ ] File versioning
- [ ] Duplicate detection
- [ ] Google OAuth
- [ ] Notifications
- [ ] Kubernetes (v3)
- [ ] Terraform (v3)

---

## 🔑 Key Technologies

### Backend:
- Java 21
- Spring Boot 4.1.0
- Spring Security 6 (JWT)
- PostgreSQL 15+
- Redis 7+
- Flyway (migrations)
- MapStruct (mapping)
- OpenAPI/Swagger

### Frontend:
- React 19
- TypeScript 6
- Vite 8
- Tailwind CSS 4
- Zustand (state)
- Axios (HTTP)
- React Router DOM 7
- React Hook Form + Zod

### Infrastructure:
- Docker + Docker Compose
- Nginx (reverse proxy)
- AWS S3 (planned)
- Kubernetes - v3
- Terraform - v3

---

## 📖 Documentation Map

**Getting Started:**
- [README.md](./README.md) - Project overview
- [Backend README](./apps/backend/README.md) - Backend setup
- [Frontend README](./apps/frontend/README.md) - Frontend setup

**Analysis & Planning:**
- [PROJECT_STATUS_ANALYSIS.md](./PROJECT_STATUS_ANALYSIS.md) - Complete status
- [FUTURE.md](./FUTURE.md) - Roadmap and priorities
- [CHANGELOG.md](./CHANGELOG.md) - Version history

**Testing:**
- [QUICK_START_TESTING.md](./QUICK_START_TESTING.md) - 5-min guide
- [docs/LOAD_TESTING_GUIDE.md](./docs/LOAD_TESTING_GUIDE.md) - Complete guide

**Documentation Index:**
- [docs/README.md](./docs/README.md) - All documentation

---

## 🎓 Learning Value

This project demonstrates:

**Backend Skills:**
- RESTful API design
- JWT authentication
- Spring Boot ecosystem
- Database design & migrations
- Redis caching strategies
- Rate limiting
- Security best practices

**Frontend Skills:**
- Modern React (hooks, context)
- TypeScript
- State management (Zustand)
- Form handling (React Hook Form)
- API integration (Axios)
- Responsive design (Tailwind)

**DevOps Skills:**
- Docker containerization
- Load testing
- Performance optimization
- Cloud deployment (AWS)
- Infrastructure as Code (planned)

**System Design:**
- Scalability (100k+ users)
- Caching strategies
- Stateless architecture
- Distributed systems
- Security architecture

---

## 🚦 Your Next Steps

1. **Read:** [README.md](./README.md) (10 min)
2. **Read:** [PROJECT_STATUS_ANALYSIS.md](./PROJECT_STATUS_ANALYSIS.md) (30 min)
3. **Read:** [FUTURE.md](./FUTURE.md) (20 min)
4. **Run:** Backend and Frontend (see above)
5. **Explore:** API docs at http://localhost:8080/swagger-ui.html
6. **Start Coding:** Pick a task from [FUTURE.md](./FUTURE.md)

---

## 💡 Tips

**For Developers:**
- Start with backend setup
- Use Swagger UI to test APIs
- Check changelogs in each module folder
- Follow existing code patterns

**For DevOps:**
- Review infrastructure folder
- Check load testing guides
- Prepare for 100k user testing

**For Managers:**
- Review PROJECT_STATUS_ANALYSIS.md
- Check FUTURE.md for timeline
- Monitor CHANGELOG.md for progress

---

## 🐛 Common Issues

**Backend won't start:**
- PostgreSQL not running? → Check port 5433
- Redis not running? → Check port 6380
- Missing JWT_SECRET? → Run generate script

**Frontend won't connect:**
- Backend not running? → Start backend first
- Wrong API URL? → Check .env file

**Tests fail:**
- Database not migrated? → Run `./mvnw flyway:migrate`

---

## 📞 Need Help?

**Check Documentation:**
1. [README.md](./README.md)
2. [PROJECT_STATUS_ANALYSIS.md](./PROJECT_STATUS_ANALYSIS.md)
3. [FUTURE.md](./FUTURE.md)
4. [Backend README](./apps/backend/README.md)
5. [Frontend README](./apps/frontend/README.md)

**Still stuck?**
- Check individual module CHANGELOGs
- Review API documentation (Swagger)
- Check code comments

---

## ✅ Quick Checklist

Before claiming you're done:

**v1 MVP:**
- [ ] All authentication working
- [ ] AWS S3 integrated ⭐
- [ ] Frontend file manager complete ⭐
- [ ] Email verification tested
- [ ] Docker deployment working ⭐
- [ ] Load tested for 100k users ⭐
- [ ] Test coverage > 60% ⭐
- [ ] Documentation updated

---

## 🎯 Success Criteria

**v1 is complete when:**
- ✅ Users can register, login, upload/download files
- ✅ Files stored in AWS S3
- ✅ Frontend file manager works
- ✅ Tested for 100,000+ concurrent users
- ✅ Deployed with Docker
- ✅ Test coverage > 60%
- ✅ Documentation complete

**Expected Timeline:** 2-3 weeks from now

---

**Welcome aboard! Start with [README.md](./README.md)** 🚀

---

*Last Updated: August 11, 2026*
