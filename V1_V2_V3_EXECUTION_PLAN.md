# ZIBOTO V1→V3 COMPLETE EXECUTION PLAN

**Created:** August 11, 2026  
**Current Status:** V1 72% Complete (S3 Integration Done ✅)  
**Target:** V3 Cloud-Native Production Platform

---

## 📊 CURRENT STATE

### Completed ✅
- Phase 0: Repository Assessment
- Phase 1: AWS S3 Integration (100% implementation, 30% testing)
- S3 manual tests passing

### In Progress 🚧
- V1 Frontend File Manager
- V1 Automated Testing
- V1 Docker Deployment

### Not Started ⏭️
- V1 Load Testing
- V2 Features (RabbitMQ, sharing, versioning, etc.)
- V3 Cloud-Native Infrastructure

---

## 🎯 EXECUTION STRATEGY

I will now implement **V1 → V2 → V3** in sequence, following this plan:

### V1: WEEK 1-3 (Production-Ready Core)
**Goal:** Working file storage platform with S3, tested, deployed

**Priorities:**
1. Frontend File Manager (2-3 days)
2. Automated Testing - 60%+ coverage (2-3 days)
3. Docker Deployment (1-2 days)
4. Email Verification Testing (1 day)
5. Load Testing & Optimization (2-3 days)

### V2: WEEK 4-8 (Feature-Complete Platform)
**Goal:** Add advanced features (sharing, versioning, RabbitMQ, RBAC)

**Priorities:**
1. RabbitMQ Integration
2. File Sharing & Permissions
3. File Versioning
4. Enhanced RBAC
5. Advanced Search
6. Notifications

### V3: WEEK 9-12 (Cloud-Native Production)
**Goal:** Deploy to AWS EKS with full observability

**Priorities:**
1. Terraform Infrastructure
2. EKS Cluster & Kubernetes
3. Helm Charts
4. Prometheus + Grafana
5. OpenTelemetry
6. CI/CD Pipeline

---

## 📋 DETAILED TASK BREAKDOWN

## V1 TASKS (Remaining)

### 1. Frontend File Manager (Days 1-3)

#### 1.1 Create fileService.ts API Client
**File:** `apps/frontend/src/services/fileService.ts`

**Methods:**
```typescript
- uploadFile(file: File, folderId?: string, onProgress?: (percent: number) => void)
- downloadFile(fileId: string, filename: string)
- listFiles(folderId?: string, page: number, size: number)
- getFileMetadata(fileId: string)
- deleteFile(fileId: string)
- searchFiles(query: string, page: number, size: number)
```

#### 1.2 Update FileManager.tsx
**File:** `apps/frontend/src/pages/FileManager.tsx`

**Features:**
- File upload with progress bar
- Drag-and-drop upload
- File list display (grid/list views)
- Folder navigation breadcrumb
- File actions (download, delete, share)
- Search functionality
- Responsive design

#### 1.3 Create Components
- `FileUploadZone.tsx` - Drag-and-drop upload
- `FileList.tsx` - Display files/folders
- `FileItem.tsx` - Individual file card
- `FileActions.tsx` - Action buttons
- `UploadProgress.tsx` - Progress indicator

---

### 2. Automated Testing (Days 4-6)

#### 2.1 S3StorageService Tests
**File:** `apps/backend/src/test/java/com/ziboto/backend/file/service/S3StorageServiceTest.java`

**Tests:**
- testUploadFile_Success()
- testUploadFile_WithMetadata()
- testDownloadFile_Success()
- testDownloadFile_NotFound()
- testDeleteFile_Success()
- testFileExists_True()
- testFileExists_False()
- testBucketAccessVerification()

#### 2.2 FileService Tests
**File:** `apps/backend/src/test/java/com/ziboto/backend/file/service/FileServiceTest.java`

**Tests:**
- testUploadFile_Success()
- testUploadFile_ExceedsQuota()
- testUploadFile_DuplicateFilename()
- testDownloadFile_Success()
- testDownloadFile_Unauthorized()
- testDeleteFile_Success()
- testListFiles_WithPagination()
- testSearchFiles_ByName()

#### 2.3 Integration Tests
**File:** `apps/backend/src/test/java/com/ziboto/backend/file/FileIntegrationTest.java`

**Tests:**
- testEndToEndFileUpload()
- testEndToEndFileDownload()
- testEndToEndFileDelete()
- testConcurrentFileUploads()
- testLargeFileUpload()

#### 2.4 Controller Tests
**File:** `apps/backend/src/test/java/com/ziboto/backend/file/controller/FileControllerTest.java`

**Tests:**
- testUploadEndpoint_Success()
- testUploadEndpoint_Unauthorized()
- testDownloadEndpoint_Success()
- testListFilesEndpoint()
- testDeleteEndpoint()

**Target Coverage:** 60%+ overall, 80%+ for critical services

---

### 3. Docker Deployment (Days 7-8)

#### 3.1 Backend Dockerfile
**File:** `apps/backend/Dockerfile`

```dockerfile
# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 3.2 Frontend Dockerfile
**File:** `apps/frontend/Dockerfile`

```dockerfile
# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1
```

#### 3.3 Docker Compose
**File:** `infra/docker/docker-compose.yml`

**Services:**
- postgres
- redis
- backend
- frontend
- nginx (reverse proxy)

#### 3.4 Nginx Configuration
**File:** `infra/docker/nginx/nginx.conf`

**Routes:**
- `/` → frontend
- `/api` → backend
- SSL/TLS configuration
- GZIP compression
- Security headers

---

### 4. Email Verification Testing (Day 9)

**Tasks:**
- Get Resend API key from user
- Update `.env` with `RESEND_API_KEY`
- Test registration email sending
- Test email verification flow
- Test password reset flow
- Test edge cases

---

### 5. Load Testing (Days 10-12)

#### 5.1 Install k6
```bash
winget install k6
```

#### 5.2 Create Test Scripts
**Files:**
- `tests/k6/login-test.js`
- `tests/k6/file-upload-test.js`
- `tests/k6/file-download-test.js`
- `tests/k6/stress-test.js`

#### 5.3 Progressive Testing
- 100 users (baseline)
- 1,000 users
- 10,000 users
- 50,000 users
- 100,000 users (if possible)

#### 5.4 Metrics to Measure
- Response times (p50, p95, p99)
- Throughput (requests/sec)
- Error rate
- Resource usage (CPU, memory, disk)

#### 5.5 Optimization
- Database query optimization
- Connection pool tuning
- Caching strategy
- CDN setup

---

## V2 TASKS (Week 4-8)

### 1. RabbitMQ Integration

**Purpose:** Async task processing, event-driven architecture

**Tasks:**
1. Add RabbitMQ dependency
2. Create RabbitMQ configuration
3. Implement message producers
4. Implement message consumers
5. File processing queue (virus scan, thumbnail generation)
6. Notification queue
7. Email queue

**Queues:**
- `file-uploaded` - Process new files
- `file-deleted` - Cleanup tasks
- `notification` - Send notifications
- `email` - Send emails

---

### 2. File Sharing & Permissions

**Features:**
- Share file with users (read-only, read-write)
- Share file via link (with expiration)
- Share folder with users
- Permission management UI
- Access control checks

**Database:**
- `file_shares` table
- `folder_shares` table
- `share_links` table

**APIs:**
- POST `/api/v1/files/{id}/share`
- GET `/api/v1/files/{id}/shares`
- DELETE `/api/v1/files/{id}/shares/{shareId}`
- POST `/api/v1/files/{id}/share-link`

---

### 3. File Versioning

**Features:**
- Auto-versioning on file update
- Version history
- Restore previous version
- Compare versions
- Version retention policy

**Database:**
- `file_versions` table

**S3 Strategy:**
- Enable S3 versioning
- Store version metadata in database
- API to list/restore versions

**APIs:**
- GET `/api/v1/files/{id}/versions`
- GET `/api/v1/files/{id}/versions/{versionId}`
- POST `/api/v1/files/{id}/restore/{versionId}`

---

### 4. Enhanced RBAC

**Roles:**
- SUPER_ADMIN
- ADMIN
- MANAGER
- USER
- GUEST

**Permissions:**
- files:create, files:read, files:update, files:delete
- files:share, files:version
- folders:create, folders:read, folders:update, folders:delete
- users:manage, roles:manage

**Implementation:**
- Spring Security with method-level security
- `@PreAuthorize` annotations
- Role hierarchy
- Permission-based access control

---

### 5. Advanced Search (Elasticsearch)

**Features:**
- Full-text search across file names and content
- Filter by file type, size, date
- Search in folders
- Search shared files
- Autocomplete suggestions

**Tech Stack:**
- Elasticsearch 8.x
- Spring Data Elasticsearch

**Indexed Fields:**
- File name
- File content (extracted text)
- File metadata
- Tags
- Upload date
- Owner

---

### 6. Duplicate Detection

**Features:**
- Detect duplicate files by SHA-256 hash
- Show duplicates to user
- Option to delete duplicates
- Save storage space

**Algorithm:**
- Calculate SHA-256 on upload
- Check for existing hash in database
- If duplicate: reference existing S3 object, create new metadata entry
- Implement reference counting for S3 cleanup

---

### 7. Notifications System

**Features:**
- Real-time notifications (WebSocket)
- Email notifications
- In-app notification center
- Notification preferences

**Events:**
- File shared with you
- File comment/mention
- Storage quota warning
- File uploaded successfully
- File processing complete

**Tech Stack:**
- WebSocket (Spring WebSocket)
- RabbitMQ for event distribution
- Database for notification storage

---

### 8. Google OAuth

**Features:**
- Login with Google
- Register with Google
- Link Google account to existing account

**Implementation:**
- Spring Security OAuth2 Client
- Google OAuth2 configuration
- Fallback to password auth

---

## V3 TASKS (Week 9-12)

### 1. Terraform Infrastructure (Week 9)

**Resources to Create:**
- VPC with public/private subnets
- EKS cluster
- RDS PostgreSQL (production database)
- ElastiCache Redis
- S3 buckets
- ALB (Application Load Balancer)
- Route53 DNS
- ACM certificates
- IAM roles and policies
- Security groups

**Files:**
- `infra/terraform/main.tf`
- `infra/terraform/vpc.tf`
- `infra/terraform/eks.tf`
- `infra/terraform/rds.tf`
- `infra/terraform/elasticache.tf`
- `infra/terraform/s3.tf`
- `infra/terraform/iam.tf`
- `infra/terraform/variables.tf`
- `infra/terraform/outputs.tf`

---

### 2. Kubernetes Manifests (Week 10)

**Resources:**
- Namespaces (dev, staging, prod)
- Deployments (backend, frontend, workers)
- Services (ClusterIP, LoadBalancer)
- Ingress (ALB Ingress Controller)
- ConfigMaps
- Secrets
- PersistentVolumeClaims
- HorizontalPodAutoscaler
- Pod Disruption Budgets
- Network Policies

**Files:**
- `infra/k8s/namespace.yaml`
- `infra/k8s/backend-deployment.yaml`
- `infra/k8s/backend-service.yaml`
- `infra/k8s/frontend-deployment.yaml`
- `infra/k8s/ingress.yaml`
- `infra/k8s/hpa.yaml`
- `infra/k8s/configmap.yaml`
- `infra/k8s/secrets.yaml` (template only)

---

### 3. Helm Charts (Week 10)

**Charts:**
- `ziboto-backend`
- `ziboto-frontend`
- `ziboto-workers`

**Files:**
- `infra/helm/ziboto/Chart.yaml`
- `infra/helm/ziboto/values.yaml`
- `infra/helm/ziboto/values-dev.yaml`
- `infra/helm/ziboto/values-prod.yaml`
- `infra/helm/ziboto/templates/`

**Benefits:**
- Easy deployment
- Environment-specific configs
- Version management
- Rollback capability

---

### 4. Observability Stack (Week 11)

#### 4.1 Prometheus
- Metrics collection
- Alerting rules
- Service discovery

#### 4.2 Grafana
- Dashboards for backend metrics
- Dashboards for infrastructure
- Dashboards for business metrics
- Alerting integration

#### 4.3 OpenTelemetry
- Distributed tracing
- Trace context propagation
- Span correlation
- Trace visualization in Grafana Tempo

#### 4.4 Logging (ELK/EFK Stack)
- Centralized logging
- Log aggregation
- Log search and analysis
- Log dashboards

#### 4.5 CloudWatch
- AWS resource monitoring
- RDS metrics
- S3 metrics
- EKS cluster metrics
- Custom application metrics

---

### 5. Resilience Patterns (Week 11)

#### 5.1 Circuit Breaker
- Resilience4j integration
- Circuit breaker for S3 calls
- Circuit breaker for database calls
- Circuit breaker for external APIs

#### 5.2 Retry Logic
- Exponential backoff
- Jitter
- Max retry attempts

#### 5.3 Rate Limiting
- API rate limiting (existing)
- Service-to-service rate limiting
- Token bucket algorithm

#### 5.4 Bulkhead
- Thread pool isolation
- Resource limits per service

#### 5.5 Timeout
- Connection timeout
- Read timeout
- Write timeout

---

### 6. CI/CD Pipeline (Week 12)

#### 6.1 GitHub Actions (or GitLab CI)

**Pipelines:**
- Build pipeline (on push)
- Test pipeline (on PR)
- Security scan pipeline
- Deploy to dev (on merge to develop)
- Deploy to staging (on merge to staging)
- Deploy to prod (on tag)

**Stages:**
1. Checkout code
2. Build backend (Maven)
3. Build frontend (npm)
4. Run tests
5. Security scan (Snyk, Trivy)
6. Build Docker images
7. Push to ECR
8. Deploy to EKS (Helm)
9. Run smoke tests
10. Notify team (Slack/Email)

#### 6.2 Deployment Strategy
- Blue-Green deployment
- Canary deployment
- Rolling updates
- Automated rollback on failure

---

## 📈 SUCCESS METRICS

### V1 Success Criteria
- ✅ All core features working
- ✅ 60%+ test coverage
- ✅ Load tested (10k+ users)
- ✅ Docker deployment working
- ✅ Production configuration validated

### V2 Success Criteria
- ✅ All V2 features working
- ✅ RabbitMQ messaging reliable
- ✅ File sharing secure
- ✅ Versioning tested
- ✅ Search performant
- ✅ 70%+ test coverage

### V3 Success Criteria
- ✅ Deployed to EKS
- ✅ Terraform infrastructure working
- ✅ Monitoring and alerting configured
- ✅ CI/CD pipeline automated
- ✅ Horizontal autoscaling working
- ✅ Production-ready

---

## 🚀 NEXT IMMEDIATE ACTIONS

### Starting Now:

1. **Frontend File Manager** (Priority 1)
   - Create fileService.ts
   - Update FileManager.tsx
   - Add upload progress
   - Add file list display

2. **Automated Testing** (Priority 2)
   - S3StorageService tests
   - FileService tests
   - Integration tests

3. **Docker Deployment** (Priority 3)
   - Backend Dockerfile
   - Frontend Dockerfile
   - docker-compose.yml
   - Nginx config

---

## ⏱️ ESTIMATED TIMELINE

```
Week 1-2: V1 Core Complete
├─ Days 1-3: Frontend File Manager
├─ Days 4-6: Automated Testing (60%+)
├─ Days 7-8: Docker Deployment
├─ Day 9: Email Verification
└─ Days 10-12: Load Testing

Week 3: V1 Hardening
├─ Security testing
├─ Performance optimization
└─ Final V1 validation

Week 4-8: V2 Features
├─ Week 4: RabbitMQ + File Sharing
├─ Week 5: Versioning + Enhanced RBAC
├─ Week 6: Advanced Search + Duplicates
├─ Week 7: Notifications + OAuth
└─ Week 8: V2 Testing & Validation

Week 9-12: V3 Cloud-Native
├─ Week 9: Terraform Infrastructure
├─ Week 10: Kubernetes + Helm
├─ Week 11: Observability + Resilience
└─ Week 12: CI/CD + Production Deploy

TOTAL: 12 weeks to V3 production
```

---

## 🎯 COMMITMENT

I will implement V1 → V2 → V3 following this plan with:
- ✅ Complete implementations (no placeholders)
- ✅ Comprehensive testing
- ✅ Honest status reporting
- ✅ Production-quality code
- ✅ Full documentation

**Let's build this! 🚀**

*Next: Start Frontend File Manager Implementation*
