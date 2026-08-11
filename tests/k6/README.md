# K6 Load Testing for Ziboto

Comprehensive load testing suite for Ziboto file storage platform.

## Installation

### Windows

**Option 1: Winget (Recommended)**
```powershell
winget install k6 --source winget
```

**Option 2: Chocolatey**
```powershell
choco install k6
```

**Option 3: Manual Download**
1. Download from https://github.com/grafana/k6/releases
2. Extract k6.exe
3. Add to PATH

### Verify Installation
```bash
k6 version
```

## Test Files

| File | Description | Duration | Users |
|------|-------------|----------|-------|
| `smoke-test.js` | Quick smoke test | 1 min | 5 |
| `load-test.js` | Standard load test | 10 min | 100 |
| `stress-test.js` | Stress test | 20 min | 500 |
| `spike-test.js` | Spike test | 15 min | 1000 |
| `endurance-test.js` | Long-running test | 60 min | 200 |
| `file-upload-test.js` | File upload specific | 10 min | 50 |
| `file-download-test.js` | File download specific | 10 min | 100 |
| `auth-test.js` | Authentication flow | 5 min | 100 |

## Running Tests

### Quick Start
```bash
# Navigate to k6 tests directory
cd tests/k6

# Run smoke test (quick validation)
k6 run smoke-test.js

# Run standard load test
k6 run load-test.js

# Run with custom VUs and duration
k6 run --vus 100 --duration 5m load-test.js
```

### Environment Variables
```bash
# Set base URL
$env:BASE_URL="http://localhost:8080"

# Set test user credentials
$env:TEST_EMAIL="loadtest@example.com"
$env:TEST_PASSWORD="Test@123"

# Run test
k6 run load-test.js
```

### Output Options
```bash
# Console output only
k6 run load-test.js

# JSON output
k6 run load-test.js --out json=results.json

# CSV output
k6 run load-test.js --out csv=results.csv

# InfluxDB (for Grafana)
k6 run load-test.js --out influxdb=http://localhost:8086/k6
```

## Test Scenarios

### 1. Smoke Test (smoke-test.js)
**Purpose:** Quick validation that system is working

**Profile:**
- 5 concurrent users
- 1 minute duration
- All endpoints tested

**Run:**
```bash
k6 run smoke-test.js
```

### 2. Load Test (load-test.js)
**Purpose:** Test system under normal load

**Profile:**
- Ramp up: 0 → 100 users (2 min)
- Sustained: 100 users (5 min)
- Ramp down: 100 → 0 users (1 min)

**Run:**
```bash
k6 run load-test.js
```

### 3. Stress Test (stress-test.js)
**Purpose:** Find breaking point

**Profile:**
- Ramp up: 0 → 500 users (5 min)
- Sustained: 500 users (10 min)
- Ramp down: 500 → 0 users (2 min)

**Run:**
```bash
k6 run stress-test.js
```

### 4. Spike Test (spike-test.js)
**Purpose:** Test sudden traffic spike

**Profile:**
- Normal: 50 users (2 min)
- Spike: 1000 users (1 min)
- Normal: 50 users (2 min)
- Spike: 1000 users (1 min)

**Run:**
```bash
k6 run spike-test.js
```

### 5. File Upload Test (file-upload-test.js)
**Purpose:** Test file upload performance

**Profile:**
- 50 concurrent users
- 10 minutes
- 1MB, 10MB, 50MB files

**Run:**
```bash
k6 run file-upload-test.js
```

## Metrics

### HTTP Metrics
- `http_req_duration` - Request duration (ms)
- `http_req_waiting` - Time to first byte (TTFB)
- `http_req_sending` - Time sending request
- `http_req_receiving` - Time receiving response
- `http_reqs` - Total HTTP requests
- `http_req_failed` - Failed request rate

### Custom Metrics
- `login_duration` - Login request time
- `upload_duration` - File upload time
- `download_duration` - File download time
- `token_refresh_duration` - Token refresh time

### Thresholds

**Good Performance:**
- p95 response time < 500ms
- p99 response time < 1000ms
- Error rate < 1%
- Requests/sec > 100

**Acceptable Performance:**
- p95 response time < 1000ms
- p99 response time < 2000ms
- Error rate < 5%
- Requests/sec > 50

## Progressive Testing Strategy

### Phase 1: Baseline (100 users)
```bash
k6 run --vus 100 --duration 5m load-test.js
```
**Goal:** Establish baseline performance

### Phase 2: Moderate Load (1,000 users)
```bash
k6 run --vus 1000 --duration 10m load-test.js
```
**Goal:** Verify system can handle moderate load

### Phase 3: High Load (10,000 users)
```bash
# May require distributed k6 or k6 cloud
k6 cloud load-test.js --vus 10000 --duration 15m
```
**Goal:** Test high concurrency

### Phase 4: Stress (50,000+ users)
```bash
# Requires k6 cloud or multiple load generators
k6 cloud stress-test.js --vus 50000 --duration 20m
```
**Goal:** Find system limits

## Distributed Load Testing

For tests > 5000 users, use distributed k6:

### Option 1: K6 Cloud
```bash
# Login to k6 cloud
k6 login cloud

# Run test on cloud
k6 cloud load-test.js --vus 50000
```

### Option 2: Multiple Machines
```bash
# Machine 1
k6 run load-test.js --vus 10000 --tag machine=1

# Machine 2
k6 run load-test.js --vus 10000 --tag machine=2

# Aggregate results later
```

## Database Preparation

Before load testing, prepare test data:

```sql
-- Create 1000 test users
DO $$
BEGIN
  FOR i IN 1..1000 LOOP
    INSERT INTO users (email, username, password_hash, full_name, email_verified, status, storage_quota_bytes, storage_used_bytes)
    VALUES (
      'loadtest' || i || '@example.com',
      'loaduser' || i,
      '$2a$10$... (bcrypt hash for "Test@123")',
      'Load Test User ' || i,
      true,
      'ACTIVE',
      5368709120,
      0
    );
  END LOOP;
END $$;

-- Create test files for download tests
INSERT INTO files (user_id, filename, storage_key, size_bytes, mime_type, sha256_hash)
SELECT 
  id,
  'test-file-' || id || '.txt',
  'files/user-' || id || '/test-file.txt',
  1048576,
  'text/plain',
  encode(sha256('test content'), 'hex')
FROM users
WHERE email LIKE 'loadtest%@example.com';
```

## Monitoring During Tests

### Backend Metrics
```bash
# CPU usage
wmic cpu get loadpercentage

# Memory usage
wmic OS get FreePhysicalMemory,TotalVisibleMemorySize

# Backend logs
tail -f apps/backend/logs/ziboto.log
```

### Database
```sql
-- Active connections
SELECT count(*) FROM pg_stat_activity;

-- Long-running queries
SELECT pid, now() - query_start as duration, query 
FROM pg_stat_activity 
WHERE state != 'idle' 
ORDER BY duration DESC;

-- Database size
SELECT pg_size_pretty(pg_database_size('ziboto'));
```

### Redis
```bash
# Monitor Redis commands
redis-cli -p 6380 monitor

# Info stats
redis-cli -p 6380 info stats

# Memory usage
redis-cli -p 6380 info memory
```

## Analyzing Results

### View Summary
```bash
k6 run load-test.js
# Summary printed at end
```

### Export to HTML
```bash
# Run with JSON output
k6 run load-test.js --out json=results.json

# Convert to HTML (requires k6-reporter)
npm install -g k6-to-html
k6-to-html results.json
```

### Key Metrics to Analyze

1. **Response Times:**
   - p50 (median)
   - p95 (95th percentile)
   - p99 (99th percentile)
   - max

2. **Throughput:**
   - Requests per second
   - Data transfer rate (MB/s)

3. **Errors:**
   - Error rate (%)
   - Error types
   - Failed requests

4. **Resource Usage:**
   - CPU usage
   - Memory usage
   - Database connections
   - Redis memory

## Optimization Targets

Based on load test results, optimize:

1. **Database:**
   - Connection pool size
   - Query optimization
   - Indexes
   - Read replicas

2. **Caching:**
   - Redis cache hit rate
   - Cache TTLs
   - Cache warming

3. **Application:**
   - Thread pool size
   - JVM heap size
   - Connection timeouts

4. **Infrastructure:**
   - Horizontal scaling
   - Load balancer config
   - CDN setup

## Troubleshooting

### k6 Command Not Found
```bash
# Verify installation
where k6

# Add to PATH if needed
$env:Path += ";C:\Program Files\k6"
```

### Connection Refused
```bash
# Check backend is running
curl http://localhost:8080/actuator/health

# Check postgres
docker ps | findstr postgres

# Check redis
docker ps | findstr redis
```

### High Error Rate
- Check backend logs: `apps/backend/logs/ziboto.log`
- Check database connections
- Check Redis memory
- Reduce VUs if overloading

### Slow Response Times
- Check database slow queries
- Check Redis latency
- Monitor CPU/memory usage
- Check network bandwidth

## CI/CD Integration

### GitHub Actions
```yaml
name: Load Test
on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  workflow_dispatch:

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Install k6
        run: |
          curl https://github.com/grafana/k6/releases/download/v0.47.0/k6-v0.47.0-linux-amd64.tar.gz -L | tar xvz
          sudo mv k6-v0.47.0-linux-amd64/k6 /usr/local/bin/
      - name: Run load test
        run: k6 run tests/k6/load-test.js
```

## Best Practices

1. **Start Small:** Begin with smoke test, gradually increase load
2. **Monitor Everything:** Backend, database, Redis, system resources
3. **Document Results:** Save metrics for comparison
4. **Test Progressively:** 100 → 1k → 10k → 50k users
5. **Use Real Data:** Test with production-like data
6. **Test Off-Hours:** Avoid impacting real users
7. **Automate:** Include load tests in CI/CD
8. **Iterate:** Test → Optimize → Test again

## References

- K6 Documentation: https://k6.io/docs/
- K6 Cloud: https://app.k6.io/
- Best Practices: https://k6.io/docs/testing-guides/
- Examples: https://github.com/grafana/k6-learn

---

**Next:** Run smoke test to validate setup
