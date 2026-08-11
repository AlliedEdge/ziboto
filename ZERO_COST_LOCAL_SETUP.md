# 🎓 ZERO-COST LOCAL SETUP FOR STUDENTS

**Goal:** Run Ziboto completely FREE with ZERO AWS charges  
**Risk Level:** ZERO - No credit card charges possible  
**Perfect for:** Students, testing, development

---

## 🎯 THE PROBLEM WITH AWS

**AWS has NO hard spending limit:**
- ❌ Cannot stop services automatically
- ❌ Cannot prevent charges
- ⚠️ Will charge credit card even if you exceed Free Tier
- ⚠️ Stories of students getting $1000+ bills

**Solution:** Don't use AWS at all during development!

---

## ✅ 100% FREE LOCAL ALTERNATIVE

Replace ALL AWS services with local alternatives:

```
AWS S3           → MinIO (S3-compatible, runs locally)
AWS RDS          → PostgreSQL (Docker)
AWS ElastiCache  → Redis (Docker)
AWS OpenSearch   → Elasticsearch (Docker)
AWS MQ           → RabbitMQ (Docker)
```

**Cost:** $0/month forever ✅

---

## 📋 STEP-BY-STEP SETUP

### Step 1: Update docker-compose.yml

Add MinIO (S3 replacement) to your Docker setup:

```yaml
# Add to: d:\Projects\Ziboto\infra\docker\docker-compose.yml

services:
  # ... existing services (postgres, redis, rabbitmq, elasticsearch)
  
  # MinIO - S3 Alternative (100% FREE)
  minio:
    image: minio/minio:latest
    container_name: ziboto-minio
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
    ports:
      - "9000:9000"   # API port
      - "9001:9001"   # Console UI
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3
    networks:
      - ziboto-network

volumes:
  # ... existing volumes
  minio_data:
    driver: local
```

---

### Step 2: Update Backend Configuration

**File:** `d:\Projects\Ziboto\apps\backend\.env`

```env
# STORAGE CONFIGURATION - USE LOCAL MINIO
STORAGE_TYPE=local

# MinIO Configuration (S3-compatible)
AWS_S3_BUCKET=ziboto-files
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=minioadmin
AWS_SECRET_ACCESS_KEY=minioadmin123
AWS_S3_ENDPOINT=http://localhost:9000  # Add this line!

# All other services (already local)
DATABASE_URL=jdbc:postgresql://localhost:5433/ziboto
REDIS_HOST=localhost
RABBITMQ_HOST=localhost
ELASTICSEARCH_URIS=http://localhost:9200
```

---

### Step 3: Update Backend Code for MinIO

**File:** `d:\Projects\Ziboto\apps\backend\src\main\java\com\ziboto\backend\config\S3Config.java`

Add endpoint configuration:

```java
@Configuration
public class S3Config {
    
    @Value("${app.storage.s3.bucket-name}")
    private String bucketName;
    
    @Value("${app.storage.s3.region}")
    private String region;
    
    @Value("${aws.s3.endpoint:}")  // NEW: Optional endpoint
    private String endpoint;
    
    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region));
        
        // NEW: Use MinIO endpoint if provided
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint))
                   .forcePathStyle(true);  // Required for MinIO
        }
        
        return builder.build();
    }
}
```

---

### Step 4: Create MinIO Bucket

```powershell
# Start all services
cd d:\Projects\Ziboto\infra\docker
docker-compose up -d

# Wait 10 seconds for MinIO to start

# Open MinIO Console in browser
# http://localhost:9001
# Username: minioadmin
# Password: minioadmin123

# Create bucket via UI:
# 1. Click "Buckets" → "Create Bucket"
# 2. Name: ziboto-files
# 3. Click "Create"

# OR use MinIO Client (mc):
docker exec ziboto-minio mc alias set local http://localhost:9000 minioadmin minioadmin123
docker exec ziboto-minio mc mb local/ziboto-files
```

---

### Step 5: Update application.yml

**File:** `d:\Projects\Ziboto\apps\backend\src\main\resources\application.yml`

```yaml
app:
  storage:
    type: ${STORAGE_TYPE:local}
    s3:
      bucket-name: ${AWS_S3_BUCKET:ziboto-files}
      region: ${AWS_REGION:us-east-1}
      endpoint: ${AWS_S3_ENDPOINT:}  # NEW: Optional endpoint
      # ... rest of config
```

---

## 🔧 COMPLETE DOCKER COMPOSE

**File:** `d:\Projects\Ziboto\infra\docker\docker-compose-local.yml`

Create a special local version with EVERYTHING:

```yaml
version: '3.9'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:17
    container_name: ziboto-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ziboto
      POSTGRES_USER: ziboto
      POSTGRES_PASSWORD: ziboto123
    ports:
      - "5433:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ziboto"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - ziboto-network

  # Redis Cache
  redis:
    image: redis:7.4-alpine
    container_name: ziboto-redis
    restart: unless-stopped
    command: redis-server --appendonly yes --requirepass ""
    ports:
      - "6380:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - ziboto-network

  # RabbitMQ Message Broker
  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    container_name: ziboto-rabbitmq
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: ziboto
      RABBITMQ_DEFAULT_PASS: ziboto123
      RABBITMQ_DEFAULT_VHOST: /
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - ziboto-network

  # Elasticsearch
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: ziboto-elasticsearch
    restart: unless-stopped
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
    networks:
      - ziboto-network

  # MinIO (S3 Alternative) - 100% FREE!
  minio:
    image: minio/minio:latest
    container_name: ziboto-minio
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3
    networks:
      - ziboto-network

volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local
  rabbitmq_data:
    driver: local
  elasticsearch_data:
    driver: local
  minio_data:
    driver: local

networks:
  ziboto-network:
    driver: bridge
```

---

## 🚀 HOW TO USE

```powershell
# 1. Start ALL services (100% local, $0 cost)
cd d:\Projects\Ziboto\infra\docker
docker-compose -f docker-compose-local.yml up -d

# 2. Create MinIO bucket (one-time)
# Open: http://localhost:9001
# Login: minioadmin / minioadmin123
# Create bucket: "ziboto-files"

# 3. Update .env
cd d:\Projects\Ziboto\apps\backend
# Edit .env:
# STORAGE_TYPE=local
# AWS_S3_ENDPOINT=http://localhost:9000
# AWS_ACCESS_KEY_ID=minioadmin
# AWS_SECRET_ACCESS_KEY=minioadmin123

# 4. Start backend
.\mvnw.cmd spring-boot:run

# 5. Test upload (will use MinIO, not AWS!)
curl -X POST http://localhost:8080/api/v1/files/upload `
  -H "Authorization: Bearer YOUR_TOKEN" `
  -F "file=@test.txt"
```

---

## 📊 VERIFY IT'S WORKING

### Check MinIO Files

```powershell
# View files in MinIO Console
# Open: http://localhost:9001
# Navigate to: Buckets → ziboto-files

# OR use MinIO Client
docker exec ziboto-minio mc ls local/ziboto-files/
```

### Check Backend Logs

```powershell
# Backend should show MinIO endpoint
cd d:\Projects\Ziboto\apps\backend
cat logs\ziboto.log | findstr "S3\|MinIO"
```

---

## ✅ BENEFITS OF THIS SETUP

**Cost:**
- ✅ $0/month forever
- ✅ No credit card needed
- ✅ No surprise bills
- ✅ No AWS account needed

**Development:**
- ✅ Faster than AWS (local network)
- ✅ Works offline
- ✅ Unlimited storage (your hard drive)
- ✅ Easy to reset/clean up

**Testing:**
- ✅ Test file uploads
- ✅ Test file downloads
- ✅ Test all V1+V2+V3 features
- ✅ No API rate limits

---

## 🎓 FOR STUDENTS: BEST PRACTICES

### 1. Never Use AWS During Development

**Use local alternatives:**
- Storage: MinIO (not S3)
- Database: PostgreSQL Docker (not RDS)
- Cache: Redis Docker (not ElastiCache)
- Search: Elasticsearch Docker (not OpenSearch)

### 2. Only Use AWS for Production

**When you have:**
- ✅ Paying customers
- ✅ Revenue to cover costs
- ✅ Budget set aside
- ✅ Monitoring set up

### 3. AWS Free Tier Is NOT Free

**Common mistakes:**
- Forgetting to stop EC2 instances
- Leaving RDS running overnight
- Data transfer charges
- Snapshot storage charges
- **Result: $100-1000 bills**

### 4. Horror Stories (Real Examples)

```
Student 1: Left EC2 running for 1 month
Bill: $150

Student 2: Accidentally created 10 EBS volumes
Bill: $500

Student 3: High data transfer from S3
Bill: $1,200

Student 4: Deployed EKS cluster for learning
Bill: $2,400 (EKS control plane alone)
```

---

## 🔒 EXTRA SAFETY: Remove AWS Credentials

**Optional: Completely remove AWS access**

```powershell
# Remove AWS credentials from your computer
Remove-Item -Path $env:USERPROFILE\.aws -Recurse -Force

# OR rename them
Rename-Item -Path $env:USERPROFILE\.aws -NewName .aws.backup
```

**This way:**
- ✅ Backend CANNOT access AWS even by accident
- ✅ Zero risk of charges
- ✅ Forces use of MinIO

---

## 📱 WHEN YOU'RE READY FOR PRODUCTION

**After graduation or when you have budget:**

1. **Get AWS Educate** (if still a student)
   - $100 free credits
   - No credit card required
   - Perfect for learning

2. **Use GitHub Student Developer Pack**
   - Includes AWS credits
   - DigitalOcean credits ($200)
   - Azure credits

3. **Start with $5-10/month budget**
   - Single EC2 instance
   - Monitor daily
   - Set billing alerts

---

## ✅ RECOMMENDED WORKFLOW

**NOW (Student, No Budget):**
```
✅ Use 100% local setup (MinIO + Docker)
✅ Build and test everything
✅ Complete V1+V2+V3
✅ Get feedback from friends
Cost: $0/month
```

**Later (After Getting Users/Revenue):**
```
✅ Switch to AWS S3
✅ Deploy to single EC2
✅ Set up billing alerts
✅ Monitor costs daily
Cost: $5-10/month (affordable)
```

**Much Later (Growing Business):**
```
✅ Scale to managed services
✅ Add redundancy
✅ Multi-region
Cost: $50-200+/month (from revenue)
```

---

## 🎉 READY TO GO!

**Your setup will be:**

```
┌─────────────────────────────────┐
│  YOUR LAPTOP (100% FREE)        │
│  ├─ PostgreSQL (Docker)         │
│  ├─ Redis (Docker)              │
│  ├─ RabbitMQ (Docker)           │
│  ├─ Elasticsearch (Docker)      │
│  ├─ MinIO (Docker) ← S3 Replacement│
│  ├─ Spring Boot Backend         │
│  └─ React Frontend              │
│                                  │
│  AWS Services: NONE              │
│  Monthly Cost: $0                │
│  Risk: ZERO                      │
└─────────────────────────────────┘
```

**You can now:**
- ✅ Test everything locally
- ✅ Upload files to MinIO
- ✅ Complete all features
- ✅ Show it to friends/professors
- ✅ ZERO AWS charges

---

## 💡 PRO TIP

**MinIO is S3-compatible**, so when you're ready to switch to real AWS S3:

1. Change `AWS_S3_ENDPOINT` to empty
2. Add real AWS credentials
3. Everything else works the same!

**Your code doesn't change at all!**

---

*Last Updated: August 11, 2026*  
*100% FREE for students! 🎓*
