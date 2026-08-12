import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
export const options = {
  vus: 5, // 5 concurrent users
  duration: '1m', // 1 minute test
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
    http_req_failed: ['rate<0.05'], // Error rate should be below 5%
    errors: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

export default function () {
  // Test 1: Health Check
  let healthRes = http.get(`${BASE_URL.replace('/api/v1', '')}/actuator/health`);
  check(healthRes, {
    'health check status is 200': (r) => r.status === 200,
    'health status is UP': (r) => r.json('status') === 'UP',
  }) || errorRate.add(1);

  sleep(1);

  // Test 2: Register
  const registerPayload = JSON.stringify({
    email: `smoketest${__VU}${Date.now()}@example.com`,
    username: `smokeuser${__VU}${Date.now()}`,
    password: 'Test@123',
    fullName: `Smoke Test User ${__VU}`,
  });

  const registerParams = {
    headers: { 'Content-Type': 'application/json' },
  };

  let registerRes = http.post(`${BASE_URL}/auth/register`, registerPayload, registerParams);
  check(registerRes, {
    'register status is 201': (r) => r.status === 201,
    'register returns success': (r) => r.json('success') === true,
  }) || errorRate.add(1);

  sleep(1);

  // Test 3: Login
  const loginPayload = JSON.stringify({
    identifier: 'testuser@example.com',
    password: 'Test@123',
  });

  let loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, registerParams);
  const loginSuccess = check(loginRes, {
    'login status is 200 or 400': (r) => r.status === 200 || r.status === 400,
  });

  if (!loginSuccess) {
    errorRate.add(1);
  }

  let token = null;
  if (loginRes.status === 200 && loginRes.json('accessToken')) {
    token = loginRes.json('accessToken');
  }

  sleep(1);

  // Test 4: Get Current User (if logged in)
  if (token) {
    const authParams = {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
    };

    let userRes = http.get(`${BASE_URL}/users/me`, authParams);
    check(userRes, {
      'get user status is 200': (r) => r.status === 200,
      'user has email': (r) => r.json('email') !== undefined,
    }) || errorRate.add(1);

    sleep(1);

    // Test 5: List Files
    let filesRes = http.get(`${BASE_URL}/files?page=0&size=10`, authParams);
    check(filesRes, {
      'list files status is 200': (r) => r.status === 200,
      'files response has content': (r) => r.json('content') !== undefined,
    }) || errorRate.add(1);

    sleep(1);

    // Test 6: List Folders
    let foldersRes = http.get(`${BASE_URL}/folders`, authParams);
    check(foldersRes, {
      'list folders status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
  }

  sleep(1);
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  const indent = options.indent || '';
  const enableColors = options.enableColors || false;
  
  let summary = '\n';
  summary += `${indent}Smoke Test Summary\n`;
  summary += `${indent}==================\n\n`;
  
  summary += `${indent}Checks:\n`;
  summary += `${indent}  ✓ Passed: ${data.metrics.checks.values.passes}\n`;
  summary += `${indent}  ✗ Failed: ${data.metrics.checks.values.fails}\n`;
  summary += `${indent}  Rate: ${(data.metrics.checks.values.rate * 100).toFixed(2)}%\n\n`;
  
  summary += `${indent}HTTP Requests:\n`;
  summary += `${indent}  Total: ${data.metrics.http_reqs.values.count}\n`;
  summary += `${indent}  Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)} req/s\n`;
  summary += `${indent}  Failed: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n\n`;
  
  summary += `${indent}Response Times:\n`;
  summary += `${indent}  Min: ${data.metrics.http_req_duration.values.min.toFixed(2)}ms\n`;
  summary += `${indent}  Avg: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
  summary += `${indent}  Med: ${data.metrics.http_req_duration.values.med.toFixed(2)}ms\n`;
  summary += `${indent}  P95: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
  summary += `${indent}  P99: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n`;
  summary += `${indent}  Max: ${data.metrics.http_req_duration.values.max.toFixed(2)}ms\n\n`;
  
  return summary;
}
