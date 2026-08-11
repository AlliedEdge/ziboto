# Quick Start: Load Testing for 100,000+ Users

**Goal:** Prove Ziboto can handle 100k+ concurrent users

---

## 🎯 Three Options (Pick One)

### Option 1: k6 (Easiest - Recommended) ⭐

**Install (5 minutes):**
```powershell
choco install k6
```

**Create test file `test.js`:**
```javascript
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 10000 },
    { duration: '5m', target: 100000 },
    { duration: '10m', target: 100000 },
    { duration: '2m', target: 0 },
  ],
};

export default function() {
  let res = http.post('http://localhost:8080/api/v1/auth/login', 
    JSON.stringify({
      email: 'test@example.com',
      password: 'Test123!'
    }), 
    { headers: { 'Content-Type': 'application/json' } }
  );
  
  check(res, {
    'login success': (r) => r.status === 200,
    'fast response': (r) => r.timings.duration < 200,
  });
}
```

**Run:**
```bash
k6 run test.js
```

**Pros:** Fast, modern, easy scripts, great reports  
**Cons:** Newer tool

---

### Option 2: JMeter (Most Popular) ⭐

**Install:**
Download from: https://jmeter.apache.org/download_jmeter.cgi

**GUI Setup:**
1. Run `jmeter.bat`
2. Add Thread Group (100,000 threads, 600s ramp-up)
3. Add HTTP Request (POST to `/api/v1/auth/login`)
4. Add listeners (Summary Report, Aggregate Report)

**CLI Run (for real testing):**
```bash
jmeter -n -t test-plan.jmx -l results.jtl -e -o report
```

**Pros:** Industry standard, huge community, GUI  
**Cons:** Java-based (heavy), slower

---

### Option 3: Gatling (High Performance)

**Install:**
Download from: https://gatling.io/open-source/

**Create simulation:**
```scala
class LoadTest extends Simulation {
  val httpProtocol = http.baseUrl("http://localhost:8080")
  
  val scn = scenario("Login Test")
    .exec(http("Login")
      .post("/api/v1/auth/login")
      .body(StringBody("""{"email":"test@example.com","password":"Test123!"}"""))
      .check(status.is(200)))
  
  setUp(scn.inject(rampUsers(100000) during (10.minutes)))
    .protocols(httpProtocol)
}
```

**Pros:** High performance, beautiful reports  
**Cons:** Scala learning curve

---

## 📊 What You're Testing

### Critical Metrics:

| What | Target | Why |
|------|--------|-----|
| **Response Time (p95)** | < 200ms | Users expect fast responses |
| **Error Rate** | < 1% | System reliability |
| **Throughput** | > 1000 req/s | Handle traffic |
| **CPU Usage** | < 80% | Room for spikes |
| **Memory** | < 80% | No memory leaks |

---

## 🔧 Before Testing: Configure Your System

### 1. Database (PostgreSQL)

Edit `application.yml`:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 200  # UP from 10
      minimum-idle: 50        # UP from 5
```

### 2. Redis

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 100     # UP from 8
          max-idle: 50        # UP from 8
```

### 3. Web Server

```yaml
server:
  tomcat:
    threads:
      max: 500              # UP from 200
      min-spare: 50
    max-connections: 10000
```

---

## 🚀 Progressive Testing Strategy

**DON'T jump straight to 100k users!**

### Step-by-Step:

1. **Test with 100 users** (baseline)
   - Fix any errors
   - Measure response times
   
2. **Test with 1,000 users** (10x)
   - Monitor database connections
   - Check Redis memory
   
3. **Test with 10,000 users** (100x)
   - Watch for bottlenecks
   - Optimize queries
   
4. **Test with 50,000 users** (500x)
   - Check server resources
   - Tune connection pools
   
5. **Test with 100,000 users** (1000x) ⭐
   - Final validation
   - Document results

---

## 📈 What Success Looks Like

### ✅ PASSING Results:

```
✓ 100,000 concurrent users handled
✓ Response time p95: 180ms (< 200ms target)
✓ Response time p99: 450ms (< 500ms target)
✓ Error rate: 0.05% (< 1% target)
✓ Throughput: 5,555 requests/second
✓ CPU: 75% (< 80% target)
✓ Memory: 70% (< 80% target)
✓ Database connections: 150/200
✓ No errors in logs
```

### ❌ FAILING Results:

```
✗ Response time p95: 2500ms (way too slow)
✗ Error rate: 15% (too many errors)
✗ Database connections: 200/200 (maxed out)
✗ OutOfMemoryError in logs
✗ CPU: 100% (system overloaded)
```

**If failing:** Optimize before claiming "supports 100k users"

---

## 🎯 Quick Commands

### k6:
```bash
# Quick test
k6 run --vus 1000 --duration 30s test.js

# Full test
k6 run test.js

# With monitoring
k6 run --out influxdb=http://localhost:8086/k6 test.js
```

### JMeter:
```bash
# Run test
jmeter -n -t test-plan.jmx -l results.jtl

# Generate report
jmeter -g results.jtl -o report-folder
```

### Gatling:
```bash
# Run test
./bin/gatling.sh

# View results
open results/index.html
```

---

## 📖 Full Documentation

- **Complete Guide:** `docs/LOAD_TESTING_GUIDE.md`
- **Future Roadmap:** `FUTURE.md`
- **Project Status:** `PROJECT_STATUS_ANALYSIS.md`

---

## ✅ Checklist Before Claiming "Supports 100k Users"

- [ ] Database connection pool configured (200+)
- [ ] Redis connection pool configured (100+)
- [ ] Tomcat threads configured (500+)
- [ ] JVM memory configured (4-8GB)
- [ ] Tested with 1k users ✓
- [ ] Tested with 10k users ✓
- [ ] Tested with 50k users ✓
- [ ] Tested with 100k users ✓
- [ ] Response time < 200ms (p95)
- [ ] Error rate < 1%
- [ ] No memory leaks
- [ ] No database connection issues
- [ ] Documented results
- [ ] Screenshots/reports saved

---

**Your Next Step:**

1. Pick a tool (recommend: k6)
2. Install it (5 minutes)
3. Run basic test with 100 users
4. Fix any issues
5. Gradually increase to 100k

**Start now:** `choco install k6` then create test.js

---

*Last Updated: August 11, 2026*
