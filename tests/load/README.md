# Ziboto k6 Load Testing

Load testing suite for Ziboto cloud storage application using k6.

## Prerequisites

Install k6: https://k6.io/docs/getting-started/installation/

```bash
# Windows (via Chocolatey)
choco install k6

# macOS
brew install k6

# Linux
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

## Test Files

### 1. `auth-load.k6.js`
Tests authentication endpoints (login, refresh token, get profile).

**Target:** 1,000 concurrent users
**Duration:** 17 minutes (5m ramp-up, 10m hold, 2m ramp-down)

```bash
k6 run auth-load.k6.js
```

### 2. `file-operations.k6.js`
Tests core file operations (list, upload, download, delete).

**Target:** 1,000 concurrent users
**Operations:** Upload 1KB files, download, list, delete

```bash
k6 run file-operations.k6.js
```

### 3. `full-scenario.k6.js`
Comprehensive test covering:
- Authentication
- User profile management
- File operations
- Storage analytics
- Activity feed

**Target:** 1,000 concurrent users
**Produces:** HTML report with visual metrics

```bash
k6 run full-scenario.k6.js
```

## Configuration

Edit `config.js` to customize:

```javascript
export const BASE_URL = 'http://your-alb-url.com';
export const VU_TARGET = 1000; // Target VUs
export const RAMP_UP_DURATION = '5m';
export const HOLD_DURATION = '10m';
```

### Environment Variables

Override configuration via environment variables:

```bash
# Test against production ALB
API_URL=http://ziboto-prod-alb-123456789.us-east-1.elb.amazonaws.com k6 run full-scenario.k6.js

# Custom VU target
VU_TARGET=500 k6 run auth-load.k6.js

# Custom durations
RAMP_UP_DURATION=3m HOLD_DURATION=5m k6 run file-operations.k6.js
```

## Running Tests

### Local Development
```bash
# Start backend locally
cd ../../apps/backend
./mvnw spring-boot:run

# Run tests (new terminal)
cd ../../tests/load
API_URL=http://localhost:8080 VU_TARGET=100 k6 run full-scenario.k6.js
```

### AWS Production
```bash
# Get ALB URL from Terraform
cd ../../infra/terraform/aws
terraform output alb_dns_name

# Run full load test
API_URL=http://<ALB_DNS> k6 run full-scenario.k6.js
```

## Success Criteria

The tests pass if they meet these thresholds:

- **Error Rate:** <1%
- **p95 Latency:** <500ms
- **p99 Latency:** <1000ms
- **Request Rate:** >100 req/s

## Interpreting Results

### Key Metrics

- `http_reqs`: Total number of requests
- `http_req_duration`: Response time distribution
- `http_req_failed`: Error rate
- `vus`: Virtual users (concurrent connections)
- `vus_max`: Peak concurrent users

### Custom Metrics

- `login_success_rate`: Percentage of successful logins
- `file_upload_success_rate`: Percentage of successful uploads
- `file_download_success_rate`: Percentage of successful downloads
- `upload_duration`: Average upload time
- `download_duration`: Average download time

### Sample Output

```
scenarios: (100.00%) 1 scenario, 1000 max VUs, 17m30s max duration

✓ login status is 200
✓ upload status is 200 or 201
✓ download status is 200

checks.........................: 95.32% ✓ 28596 ✗ 1404
data_received..................: 245 MB  239 kB/s
data_sent......................: 82 MB   80 kB/s
http_req_duration..............: avg=325ms min=45ms med=280ms max=2.1s p(95)=489ms p(99)=865ms
http_reqs......................: 142980  140/s
vus............................: 1000    min=0    max=1000
vus_max........................: 1000    min=1000 max=1000
```

## Test Scenarios

### Staged Load Test (Default)
Gradually increases load to simulate real-world traffic growth.

### Spike Test
```bash
k6 run --config spike-test.json full-scenario.k6.js
```
Sudden traffic spike to test system resilience.

### Stress Test
```bash
k6 run --config stress-test.json full-scenario.k6.js
```
Pushes system beyond normal limits to find breaking point.

## CloudWatch Integration

Monitor backend performance during load tests:

```bash
# View EC2 CPU utilization
aws cloudwatch get-metric-statistics \
  --namespace AWS/EC2 \
  --metric-name CPUUtilization \
  --dimensions Name=AutoScalingGroupName,Value=ziboto-prod-backend-asg \
  --start-time 2026-08-15T00:00:00Z \
  --end-time 2026-08-15T23:59:59Z \
  --period 300 \
  --statistics Average

# View ALB request count
aws cloudwatch get-metric-statistics \
  --namespace AWS/ApplicationELB \
  --metric-name RequestCount \
  --dimensions Name=LoadBalancer,Value=app/ziboto-prod-alb/* \
  --start-time 2026-08-15T00:00:00Z \
  --end-time 2026-08-15T23:59:59Z \
  --period 60 \
  --statistics Sum
```

## Troubleshooting

### High Error Rate
- Check backend logs: `aws logs tail /aws/ec2/ziboto-backend --follow`
- Verify RDS connections: CloudWatch metric `DatabaseConnections`
- Check Redis memory: CloudWatch metric `DatabaseMemoryUsagePercentage`

### High Latency
- Check ALB target health: `aws elbv2 describe-target-health --target-group-arn <ARN>`
- Verify EC2 instance count: Backend may need more instances
- Check database performance: RDS Performance Insights

### Connection Errors
- Verify ALB security group allows inbound HTTP/HTTPS
- Check backend security group allows ALB traffic
- Verify EC2 instances are registered with target group

## Best Practices

1. **Start Small:** Begin with 10-50 VUs to verify setup
2. **Warm Up:** Let system warm up before measuring (JVM JIT, connection pools)
3. **Clean Data:** Delete test files periodically to avoid storage bloat
4. **Monitor Resources:** Watch CloudWatch during tests
5. **Test Incrementally:** Gradually increase load to find limits
6. **Document Results:** Save reports for comparison over time

## Reports

Test results are saved to:
- `auth-load-summary.json`
- `file-operations-summary.json`
- `full-scenario-summary.json`
- `full-scenario-summary.html` (visual report)

Open HTML reports in browser for detailed analysis.
