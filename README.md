# Ziboto Cloud Storage

A production-ready, horizontally scalable cloud storage platform built with Spring Boot and React, deployed on AWS with clean architecture.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AWS](https://img.shields.io/badge/AWS-Deployed-FF9900.svg)](https://aws.amazon.com/)

**Live Demo:** [Coming Soon]

---

## 🎯 Project Overview

Ziboto is a **Google Drive-style cloud storage application** designed to demonstrate:

- ✅ Full-stack development (React + Spring Boot)
- ✅ AWS cloud deployment and architecture
- ✅ Horizontal backend scaling (2-10 EC2 instances)
- ✅ Application Load Balancer for high availability
- ✅ Infrastructure as Code (Terraform)
- ✅ Load testing with k6 (1,000+ concurrent users)
- ✅ Production-ready security and monitoring
- ✅ Cost-aware AWS resource management

**Perfect for:** Technical interviews, portfolio projects, learning cloud architecture, and understanding scalable backend systems.

---

## 🏗️ Architecture

```
Users
  ↓
AWS Amplify (Frontend - React)
  ↓
Application Load Balancer
  ↓
┌──────────────────┬──────────────────┐
│   EC2 Instance 1 │   EC2 Instance 2 │  (Auto-scales 2-10)
│   Backend        │   Backend        │
│   Spring Boot    │   Spring Boot    │
└──────────────────┴──────────────────┘
  ↓
┌─────────────────┬──────────────┬─────────────────┐
│                 │              │                 │
RDS PostgreSQL    Amazon S3      Amazon MQ         
Multi-AZ          File Storage   RabbitMQ          
(metadata)        (files)        (async jobs)
                  
Redis ElastiCache
(sessions, cache)
```

### Why This Architecture?

- **EC2 + ALB** instead of Kubernetes: Simpler, easier to explain, production-ready
- **Horizontal Scaling:** Auto Scaling Group handles 2-10 backend instances
- **Stateless Backend:** Can scale horizontally without session affinity issues
- **Managed Services:** RDS, ElastiCache, Amazon MQ reduce operational overhead
- **Cost-Optimized:** ~$50-130/month, can stop resources when not in use

---

## ✨ Features

### 📁 Core File Management
- Upload, download, delete files
- Hierarchical folder structure
- File versioning and history
- 30-day trash bin with restore
- Duplicate detection (SHA-256)
- File preview (images, PDFs, videos, code)

### 🔐 Security & Authentication
- JWT-based authentication (access + refresh tokens)
- Role-based access control (RBAC)
- Email verification
- Password reset
- Rate limiting (Redis-based)
- Audit logging

### 🤝 Sharing & Collaboration
- Public/private share links
- Password-protected shares
- Expiring links
- File commenting with threading
- Public galleries

### ⚡ Performance
- Redis caching for sessions and frequently accessed data
- PostgreSQL with connection pooling
- S3 for scalable object storage
- Asynchronous processing with RabbitMQ
- CloudWatch monitoring and alarms

### 📊 Analytics
- Storage usage tracking
- Activity feed
- User analytics dashboard

---

## 🛠️ Technology Stack

### Backend
- **Framework:** Spring Boot 3.x (Java 21)
- **Security:** Spring Security, JWT (JJWT)
- **Database:** PostgreSQL 15 (JPA/Hibernate)
- **Cache:** Redis 7
- **Messaging:** RabbitMQ 3.12
- **Storage:** AWS S3
- **Email:** Resend SDK
- **API Docs:** SpringDoc OpenAPI

### Frontend
- **Framework:** React 19 + Vite
- **State:** Zustand
- **Routing:** React Router v7
- **HTTP:** Axios
- **UI:** Tailwind CSS v4, Framer Motion
- **Forms:** React Hook Form + Zod

### Infrastructure
- **Cloud:** AWS (EC2, ALB, RDS, S3, ElastiCache, Amazon MQ)
- **IaC:** Terraform
- **Deployment:** AWS Amplify (frontend), EC2 Auto Scaling (backend)
- **Monitoring:** CloudWatch, Budget alerts
- **Load Testing:** k6

---

## 🚀 Quick Start

### Prerequisites
- Java 21
- Node.js 18+
- Docker & Docker Compose
- AWS CLI (for deployment)
- Terraform 1.5+ (for infrastructure)

### Local Development

1. **Clone Repository**
```bash
git clone https://github.com/yourusername/ziboto.git
cd ziboto
```

2. **Start Infrastructure Services**
```bash
cd infra/docker
docker-compose up -d postgres redis rabbitmq
```

3. **Configure Backend**
```bash
cd ../../apps/backend
cp .env.example .env
# Edit .env with your configuration
```

4. **Run Backend**
```bash
./mvnw spring-boot:run
```

Backend runs on http://localhost:8080

5. **Configure Frontend**
```bash
cd ../frontend
cp .env.example .env
# Edit .env: VITE_API_URL=http://localhost:8080/api/v1
```

6. **Run Frontend**
```bash
npm install
npm run dev
```

Frontend runs on http://localhost:5173

7. **Access Application**
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html
- RabbitMQ Management: http://localhost:15672 (ziboto/ziboto123)

---

## 📦 AWS Deployment

Complete deployment guide: [DEPLOYMENT.md](DEPLOYMENT.md)

### Quick Deploy

```bash
cd infra/terraform/aws

# 1. Configure variables
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your settings

# 2. Deploy infrastructure (15-20 minutes)
terraform init
terraform apply

# 3. Build and upload backend
cd ../../../apps/backend
./mvnw clean package -DskipTests
aws s3 cp target/backend-*.jar s3://$(terraform output -raw s3_bucket_name)/deploy/ziboto-backend.jar

# 4. Deploy frontend to Amplify
cd ../frontend
amplify init
amplify publish
```

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed instructions.

---

## 🧪 Load Testing

k6 load testing suite included for performance validation.

```bash
cd tests/load

# Authentication test
k6 run auth-load.k6.js

# File operations test
k6 run file-operations.k6.js

# Full scenario (1,000 VUs)
API_URL=http://your-alb-url.com k6 run full-scenario.k6.js
```

**Target Performance:**
- 1,000 concurrent users
- <1% error rate
- p95 latency <500ms
- p99 latency <1000ms

See [tests/load/README.md](tests/load/README.md) for details.

---

## 💰 Cost Management

Estimated AWS costs: **$50-130/month**

### Always Running:
- RDS PostgreSQL: ~$15/month
- ElastiCache Redis: ~$12/month
- Amazon MQ RabbitMQ: ~$17/month
- ALB: ~$22/month
- NAT Gateways: ~$64/month

### Variable:
- EC2 instances (2x t3.medium): ~$60/month
- S3 storage: ~$0.023/GB
- Data transfer: ~$0.09/GB

### Cost Optimization:
- Stop EC2 when not demonstrating (saves $60/month)
- Stop RDS for up to 7 days (saves 50%)
- Delete ElastiCache/Amazon MQ when not testing (saves $29/month)
- Use t3.micro instead of t3.medium (saves $30/month)

**AWS Budget configured** with alerts at 50%, 80%, 100% of monthly limit.

---

## 📊 Horizontal Scaling

### Auto Scaling Configuration
- **Minimum:** 2 instances (high availability)
- **Maximum:** 10 instances (cost control)
- **Scale Up:** CPU > 70% for 2 minutes → add 1 instance
- **Scale Down:** CPU < 20% for 2 minutes → remove 1 instance

### Load Distribution
```
       Application Load Balancer
              ↓
    ┌─────────┼─────────┐
    ↓         ↓         ↓
  EC2 #1    EC2 #2    EC2 #3
  Backend   Backend   Backend
```

### Stateless Design
- **Sessions:** Redis (shared across instances)
- **Files:** S3 (centralized)
- **Database:** RDS (connection pooling)
- **Messages:** Amazon MQ (shared queue)

---

## 🔐 Security

- **Authentication:** JWT with refresh tokens
- **Authorization:** Role-based access control (RBAC)
- **Rate Limiting:** Redis-based (5 login attempts per 15 min)
- **Encryption at Rest:** RDS, S3, ElastiCache, Amazon MQ (KMS)
- **Encryption in Transit:** HTTPS (optional), SSL for database
- **Network Security:** Private subnets, security groups
- **No Hardcoded Secrets:** IAM roles, environment variables
- **Audit Logging:** Comprehensive audit trail

---

## 📁 Project Structure

```
ziboto/
├── apps/
│   ├── backend/            # Spring Boot backend
│   │   ├── src/main/java/com/ziboto/backend/
│   │   │   ├── auth/       # JWT authentication
│   │   │   ├── file/       # File management
│   │   │   ├── user/       # User management
│   │   │   ├── share/      # File sharing
│   │   │   ├── messaging/  # RabbitMQ
│   │   │   ├── cache/      # Redis
│   │   │   └── ...         # 21 modules total
│   │   └── pom.xml
│   └── frontend/           # React frontend
│       ├── src/
│       └── package.json
├── infra/
│   ├── terraform/aws/      # AWS infrastructure
│   │   ├── main.tf
│   │   ├── ec2.tf
│   │   ├── alb.tf
│   │   ├── rds.tf
│   │   ├── s3.tf
│   │   └── ...
│   └── docker/             # Local development
│       └── docker-compose.yml
├── tests/load/             # k6 load tests
├── DEPLOYMENT.md           # Deployment guide
├── RESTRUCTURING_SUMMARY.md # Architecture decisions
└── README.md               # This file
```

---

## 📖 Documentation

- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Complete AWS deployment guide
- **[RESTRUCTURING_SUMMARY.md](RESTRUCTURING_SUMMARY.md)** - Architecture decisions and changes
- **[tests/load/README.md](tests/load/README.md)** - Load testing guide
- **[Terraform README](infra/terraform/aws/README.md)** - Infrastructure configuration

---

## 🎓 Learning Outcomes

This project demonstrates:

1. **Full-Stack Development**
   - RESTful API design
   - React SPA with state management
   - Form validation, error handling
   - File upload/download with progress

2. **Cloud Architecture**
   - Horizontal scaling with ALB + Auto Scaling
   - Managed services (RDS, ElastiCache, Amazon MQ)
   - Stateless backend design
   - Multi-AZ high availability

3. **Infrastructure as Code**
   - Terraform for AWS resources
   - Modular infrastructure design
   - Environment separation (dev/prod)

4. **DevOps Practices**
   - CI/CD with AWS Amplify
   - Automated deployments
   - CloudWatch monitoring
   - Log aggregation

5. **Performance & Scalability**
   - Load balancing
   - Caching strategies (Redis)
   - Asynchronous processing (RabbitMQ)
   - Database optimization

6. **Security**
   - Authentication & authorization
   - Encryption (at rest & in transit)
   - Network isolation
   - Audit logging

7. **Cost Management**
   - Resource optimization
   - Budget alerts
   - Right-sizing instances
   - Stopping idle resources

---

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

Built with:
- [Spring Boot](https://spring.io/projects/spring-boot)
- [React](https://react.dev/)
- [PostgreSQL](https://www.postgresql.org/)
- [Redis](https://redis.io/)
- [RabbitMQ](https://www.rabbitmq.com/)
- [AWS](https://aws.amazon.com/)
- [Terraform](https://www.terraform.io/)
- [k6](https://k6.io/)

---

## 📧 Contact

For questions or feedback, please open an issue on GitHub.

---

**Ziboto** - Production-ready cloud storage with clean AWS architecture.