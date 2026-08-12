# 🧪 ZIBOTO LOCAL TESTING GUIDE

**Purpose:** Test the complete Ziboto application locally before AWS deployment  
**AWS Usage:** S3 only (minimal cost)  
**Prerequisites:** Docker Desktop, AWS credentials configured

---

## ✅ CURRENT STATUS

**Backend:** ✅ Complete (V1 + V2 features)  
**Database:** ✅ PostgreSQL in Docker  
**Cache:** ✅ Redis in Docker  
**Message Queue:** ✅ RabbitMQ in Docker  
**Search:** ✅ Elasticsearch in Docker  
**Storage:** ✅ AWS S3 (only service using AWS credits)  
**Frontend:** ⚠️ Needs verification

---

## 📋 STEP-BY-STEP TESTING

### Step 1: Verify Docker Desktop is Running

```powershell
# Check Docker is running
docker --version
docker ps
```

---

### Step 2: Start Infrastructure Services

```powershell
cd d:\Projects\Ziboto\infra\docker

# Start all services
docker-compose up -d

# Verify all services are running
docker-compose ps
```

**Expected Services:**
- ✅ ziboto-postgres (port 5433)
- ✅ ziboto-redis (port 6380)
- ✅ ziboto-rabbitmq (port 5672, 15672)
- ✅ ziboto-elasticsearch (port 9200, 9300)

**Verify Services:**
```powershell
# Check PostgreSQL
docker exec -it ziboto-postgres psql -U ziboto -d ziboto -c "SELECT 1;"

# Check Redis
docker exec -it ziboto-redis redis-cli ping

# Check RabbitMQ Management UI
# Open: http://localhost:15672
# Username: ziboto
# Password: ziboto123

# Check Elasticsearch
curl http://localhost:9200
```

---

### Step 3: Configure Backend Environment

```powershell
cd d:\Projects\Ziboto\apps\backend

# Copy .env.example to .env (if not already done)
copy .env.example .env

# Edit .env file - IMPORTANT SETTINGS:
```

**Required .env values:**
```env
# Database (Docker)
DATABASE_URL=jdbc:postgresql://localhost:5433/ziboto
DATABASE_USERNAME=ziboto
DATABASE_PASSWORD=ziboto123

# Redis (Docker)
REDIS_HOST=localhost
REDIS_PORT=6380
REDIS_PASSWORD=

# RabbitMQ (Docker)
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=ziboto
RABBITMQ_PASSWORD=ziboto123

# Elasticsearch (Docker)
ELASTICSEARCH_URIS=http://localhost:9200
ELASTICSEARCH_USERNAME=
ELASTICSEARCH_PASSWORD=

# Storage (AWS S3 - ONLY AWS SERVICE)
STORAGE_TYPE=s3
AWS_REGION=eu-north-1
AWS_S3_BUCKET=ziboto-files-277522752099-eu-north-1-an

# JWT Secret (generate new one if needed)
JWT_SECRET=HJYuXxKYFT4euP13F4LSLpOy04amKn02lRdtlM4eDr2SmdPa6Sd7rlooTOzeBqDVRobztLHBBhrazss0kzXerA==

# Email (Optional for now)
RESEND_API_KEY=your_key_here_or_leave_empty
RESEND_FROM_EMAIL=noreply@yourdomain.com
```

---

### Step 4: Run Database Migrations

The backend will automatically run Flyway migrations on startup, but you can verify:

```powershell
cd d:\Projects\Ziboto\apps\backend

# Build and run migrations
.\mvnw.cmd clean compile flyway:migrate
```

**Expected Migrations:**
- V1__init.sql
- V2__user_roles_permissions.sql
- V3__create_file_metadata_table.sql
- ... (up to V17__activity_logs.sql)

---

### Step 5: Start Backend Server

```powershell
cd d:\Projects\Ziboto\apps\backend

# Option 1: Run with Maven (Development)
.\mvnw.cmd spring-boot:run

# Option 2: Build JAR and run
.\mvnw.cmd clean package -DskipTests
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

**Expected Output:**
```
Started ZibotoBackendApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

**Verify Backend:**
```powershell
# Health check
curl http://localhost:8080/actuator/health

# API documentation
# Open: http://localhost:8080/swagger-ui.html
```

---

### Step 6: Test Backend APIs

#### 6.1 Register a User

```powershell
# Using curl
curl -X POST http://localhost:8080/api/v1/auth/register `
  -H "Content-Type: application/json" `
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!@#",
    "firstName": "Test",
    "lastName": "User"
  }'
```

#### 6.2 Login

```powershell
curl -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{
    "username": "testuser",
    "password": "Test123!@#"
  }'
```

**Save the JWT token from response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "...",
  "tokenType": "Bearer"
}
```

#### 6.3 Upload a File to S3

```powershell
# Set your token
$token = "YOUR_JWT_TOKEN_HERE"

# Upload a test file
curl -X POST http://localhost:8080/api/v1/files/upload `
  -H "Authorization: Bearer $token" `
  -F "file=@C:\path\to\test-file.txt"
```

**✅ This will use S3 - your only AWS cost!**

#### 6.4 List Files

```powershell
curl -X GET http://localhost:8080/api/v1/files `
  -H "Authorization: Bearer $token"
```

#### 6.5 Search Files (Elasticsearch)

```powershell
curl -X POST http://localhost:8080/api/v1/search `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d '{
    "query": "test",
    "page": 0,
    "size": 20
  }'
```

---

### Step 7: Check Frontend (if exists)

```powershell
cd d:\Projects\Ziboto\apps\frontend

# Install dependencies (if not done)
npm install

# Start development server
npm run dev
```

**Expected Output:**
```
VITE v5.x.x ready in XXX ms
Local: http://localhost:5173/
```

**Open browser:** http://localhost:5173

---

## 🔍 MONITORING TOOLS

### RabbitMQ Management UI
- **URL:** http://localhost:15672
- **Username:** ziboto
- **Password:** ziboto123
- **Check:** Queues, messages, connections

### Elasticsearch
```powershell
# Check indices
curl http://localhost:9200/_cat/indices?v

# Check file documents
curl http://localhost:9200/files/_search?pretty
```

### PostgreSQL (via Docker)
```powershell
# Connect to database
docker exec -it ziboto-postgres psql -U ziboto -d ziboto

# List tables
\dt

# Check files
SELECT id, file_name, file_size, created_at FROM file_metadata LIMIT 10;

# Check activity logs (V3)
SELECT * FROM activity_logs ORDER BY created_at DESC LIMIT 10;

# Exit
\q
```

---

## 💰 AWS COST MONITORING

### Check Current S3 Usage

```powershell
# Using AWS CLI
aws s3 ls s3://ziboto-files-277522752099-eu-north-1-an/

# Get bucket size
aws s3 ls s3://ziboto-files-277522752099-eu-north-1-an/ --recursive --summarize
```

### Monitor Costs
1. Open AWS Console: https://console.aws.amazon.com
2. Go to **Cost Explorer**
3. View **Current Month** spending
4. Set up **Billing Alerts**:
   - Go to CloudWatch
   - Create alarm when billing exceeds $5, $10, etc.

### Clean Up Test Files
```powershell
# List files in S3
aws s3 ls s3://ziboto-files-277522752099-eu-north-1-an/files/

# Delete specific file
aws s3 rm s3://ziboto-files-277522752099-eu-north-1-an/files/xxx/yyy

# Delete all files (CAREFUL!)
# aws s3 rm s3://ziboto-files-277522752099-eu-north-1-an/files/ --recursive
```

---

## 🚨 TROUBLESHOOTING

### Backend Won't Start

**Check logs:**
```powershell
# Check application logs
cd d:\Projects\Ziboto\apps\backend
cat logs\ziboto.log
```

**Common issues:**
1. **Port 8080 already in use:**
   ```powershell
   # Find process using port 8080
   netstat -ano | findstr :8080
   # Kill process
   taskkill /F /PID <PID>
   ```

2. **Database connection failed:**
   - Verify Docker is running
   - Check PostgreSQL container: `docker ps | findstr postgres`
   - Restart container: `docker restart ziboto-postgres`

3. **S3 access denied:**
   - Verify AWS credentials: `aws sts get-caller-identity`
   - Check bucket policy in AWS console

---

### Docker Services Not Starting

```powershell
# Check Docker Desktop is running

# View logs
docker-compose logs postgres
docker-compose logs redis
docker-compose logs rabbitmq
docker-compose logs elasticsearch

# Restart all services
docker-compose restart

# Nuclear option: remove and recreate
docker-compose down -v
docker-compose up -d
```

---

### Elasticsearch Out of Memory

```powershell
# Check Elasticsearch logs
docker logs ziboto-elasticsearch

# Increase heap size in docker-compose.yml:
# ES_JAVA_OPTS: -Xms512m -Xmx512m  # Change to 1g if needed
```

---

## ✅ TESTING CHECKLIST

### Backend API Tests
- [ ] User registration works
- [ ] User login works
- [ ] JWT token received
- [ ] File upload to S3 works
- [ ] File download works
- [ ] File delete works
- [ ] File search (Elasticsearch) works
- [ ] RabbitMQ messages sent (check management UI)
- [ ] Activity logs created (check database)

### Infrastructure Tests
- [ ] PostgreSQL accessible
- [ ] Redis ping successful
- [ ] RabbitMQ management UI accessible
- [ ] Elasticsearch health check green
- [ ] S3 bucket accessible

### Frontend Tests (if available)
- [ ] Login page loads
- [ ] User can register
- [ ] User can login
- [ ] File manager page loads
- [ ] Files displayed correctly
- [ ] File upload works with progress bar
- [ ] File download works

---

## 📊 EXPECTED AWS COSTS (During Local Testing)

**S3 Storage:**
- 0 GB: $0.00/month
- 1 GB: $0.023/month (~2 cents)
- 10 GB: $0.23/month (23 cents)
- 100 GB: $2.30/month

**S3 Requests:**
- PUT/POST: $0.005 per 1,000 requests
- GET: $0.0004 per 1,000 requests
- 1,000 uploads: $0.005 (half a cent)

**Total Expected Cost for Testing:**
- **$0.10 - $1.00 per month** (very minimal!)

---

## 🎯 NEXT STEPS AFTER TESTING

Once local testing is complete:

1. ✅ **Verify all features work**
2. ✅ **Clean up test data from S3**
3. ⏭️ **Continue with V3 implementation** (Activity Feed, etc.)
4. ⏭️ **Add frontend improvements**
5. ⏭️ **Prepare for AWS deployment** (ECS/EKS)

---

## 💡 COST-SAVING TIPS FOR AWS DEPLOYMENT

When ready to deploy:

1. **Use AWS Free Tier:**
   - EC2: 750 hours/month (t2.micro/t3.micro)
   - RDS: 750 hours/month (db.t2.micro)
   - S3: 5 GB storage, 20,000 GET, 2,000 PUT
   - Valid for 12 months for new accounts

2. **Use Spot Instances:**
   - 70-90% cheaper than on-demand
   - Good for non-critical workloads

3. **Use ECS Instead of EKS:**
   - ECS: Free (only pay for EC2/Fargate)
   - EKS: $0.10/hour = $73/month just for control plane

4. **Set Budget Alerts:**
   ```powershell
   # Set budget via AWS CLI
   aws budgets create-budget --account-id YOUR_ACCOUNT_ID \
     --budget file://budget.json
   ```

5. **Use LocalStack for Development:**
   - Emulates AWS services locally
   - Zero AWS costs during development
   - We can set this up later

---

## 🎉 READY TO TEST!

**You can start testing NOW!**

**Commands to run:**
```powershell
# 1. Start Docker services
cd d:\Projects\Ziboto\infra\docker
docker-compose up -d

# 2. Wait 30 seconds for services to be ready

# 3. Start backend
cd d:\Projects\Ziboto\apps\backend
.\mvnw.cmd spring-boot:run

# 4. In another terminal, test API
curl http://localhost:8080/actuator/health

# 5. Open Swagger UI in browser
# http://localhost:8080/swagger-ui.html
```

**AWS Cost: ~$0.10-$1.00/month** (just S3 for testing)

---

*Last Updated: August 11, 2026*  
*Ready for local testing! 🚀*
