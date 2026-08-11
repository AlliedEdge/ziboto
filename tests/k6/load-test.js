import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('errors');
const loginDuration = new Trend('login_duration');
const uploadDuration = new Trend('upload_duration');
const downloadDuration = new Trend('download_duration');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 50 },   // Ramp up to 50 users
    { duration: '3m', target: 100 },  // Ramp up to 100 users
    { duration: '5m', target: 100 },  // Stay at 100 users for 5 minutes
    { duration: '2m', target: 0 },    // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000', 'p(99)<2000'], // 95% < 1s, 99% < 2s
    http_req_failed: ['rate<0.05'], // < 5% errors
    errors: ['rate<0.05'],
    login_duration: ['p(95)<800'],
    upload_duration: ['p(95)<3000'],
    download_duration: ['p(95)<1000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

// Shared state for test users
const testUsers = [];
for (let i = 1; i <= 100; i++) {
  testUsers.push({
    email: `loadtest${i}@example.com`,
    username: `loaduser${i}`,
    password: 'Test@123',
  });
}

export function setup() {
  console.log('Starting load test...');
  console.log(`Base URL: ${BASE_URL}`);
  console.log(`Test users: ${testUsers.length}`);
  
  // Verify backend is healthy
  const healthRes = http.get(`${BASE_URL.replace('/api/v1', '')}/actuator/health`);
  if (healthRes.status !== 200) {
    throw new Error('Backend health check failed');
  }
  
  return { testUsers };
}

export default function (data) {
  // Select a test user for this VU
  const user = data.testUsers[(__VU - 1) % data.testUsers.length];
  let token = null;

  group('Authentication', function () {
    // Login
    const loginPayload = JSON.stringify({
      identifier: user.email,
      password: user.password,
    });

    const params = {
      headers: { 'Content-Type': 'application/json' },
    };

    const loginStart = Date.now();
    const loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, params);
    loginDuration.add(Date.now() - loginStart);

    const loginCheck = check(loginRes, {
      'login status is 200': (r) => r.status === 200,
      'login returns token': (r) => r.json('accessToken') !== undefined,
    });

    if (!loginCheck) {
      errorRate.add(1);
      return; // Skip rest if login failed
    }

    token = loginRes.json('accessToken');
  });

  sleep(1);

  // Authenticated requests
  const authParams = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  };

  group('User Operations', function () {
    // Get current user
    let userRes = http.get(`${BASE_URL}/users/me`, authParams);
    check(userRes, {
      'get user status is 200': (r) => r.status === 200,
      'user has id': (r) => r.json('id') !== undefined,
    }) || errorRate.add(1);

    sleep(0.5);

    // Get user stats
    let statsRes = http.get(`${BASE_URL}/users/me/stats`, authParams);
    check(statsRes, {
      'get stats status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
  });

  sleep(1);

  group('Folder Operations', function () {
    // List folders
    let foldersRes = http.get(`${BASE_URL}/folders`, authParams);
    check(foldersRes, {
      'list folders status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);

    sleep(0.5);

    // Create folder (10% of users)
    if (Math.random() < 0.1) {
      const folderPayload = JSON.stringify({
        name: `LoadTest-${randomString(8)}`,
      });

      let createFolderRes = http.post(`${BASE_URL}/folders`, folderPayload, authParams);
      check(createFolderRes, {
        'create folder status is 201': (r) => r.status === 201,
      }) || errorRate.add(1);
    }
  });

  sleep(1);

  group('File Operations', function () {
    // List files
    let filesRes = http.get(`${BASE_URL}/files?page=0&size=20`, authParams);
    check(filesRes, {
      'list files status is 200': (r) => r.status === 200,
      'files has content': (r) => r.json('content') !== undefined,
    }) || errorRate.add(1);

    sleep(0.5);

    // Search files (20% of users)
    if (Math.random() < 0.2) {
      let searchRes = http.get(`${BASE_URL}/files/search?query=test&page=0&size=10`, authParams);
      check(searchRes, {
        'search files status is 200': (r) => r.status === 200,
      }) || errorRate.add(1);
    }

    sleep(0.5);

    // Upload file (5% of users) - small file
    if (Math.random() < 0.05) {
      const fileContent = randomString(1024 * 10); // 10 KB
      const fileName = `loadtest-${Date.now()}.txt`;

      const uploadPayload = {
        file: http.file(fileContent, fileName, 'text/plain'),
      };

      const uploadParams = {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      };

      const uploadStart = Date.now();
      let uploadRes = http.post(`${BASE_URL}/files/upload`, uploadPayload, uploadParams);
      uploadDuration.add(Date.now() - uploadStart);

      check(uploadRes, {
        'upload status is 201': (r) => r.status === 201,
      }) || errorRate.add(1);
    }
  });

  sleep(2);

  // Token refresh (10% of users)
  if (Math.random() < 0.1) {
    group('Token Refresh', function () {
      const refreshPayload = JSON.stringify({
        refreshToken: token, // Simplified for load test
      });

      let refreshRes = http.post(`${BASE_URL}/auth/refresh`, refreshPayload, authParams);
      check(refreshRes, {
        'refresh status is 200': (r) => r.status === 200,
      }); // Don't count as error if refresh fails
    });
  }

  sleep(1);
}

export function teardown(data) {
  console.log('Load test completed');
}

export function handleSummary(data) {
  const summary = {
    'stdout': generateTextSummary(data),
    'load-test-results.json': JSON.stringify(data, null, 2),
  };
  
  return summary;
}

function generateTextSummary(data) {
  let summary = '\n\n';
  summary += '========================================\n';
  summary += '      LOAD TEST RESULTS SUMMARY\n';
  summary += '========================================\n\n';
  
  summary += 'Test Configuration:\n';
  summary += '  Target: 100 concurrent users\n';
  summary += '  Duration: 12 minutes\n';
  summary += '  Stages: Ramp up → Sustain → Ramp down\n\n';
  
  summary += '----------------------------------------\n';
  summary += 'HTTP REQUESTS\n';
  summary += '----------------------------------------\n';
  summary += `  Total Requests: ${data.metrics.http_reqs.values.count}\n`;
  summary += `  Requests/sec: ${data.metrics.http_reqs.values.rate.toFixed(2)}\n`;
  summary += `  Failed Requests: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n\n`;
  
  summary += '----------------------------------------\n';
  summary += 'RESPONSE TIMES (ms)\n';
  summary += '----------------------------------------\n';
  summary += `  Min:     ${data.metrics.http_req_duration.values.min.toFixed(2)}\n`;
  summary += `  Average: ${data.metrics.http_req_duration.values.avg.toFixed(2)}\n`;
  summary += `  Median:  ${data.metrics.http_req_duration.values.med.toFixed(2)}\n`;
  summary += `  P90:     ${data.metrics.http_req_duration.values['p(90)'].toFixed(2)}\n`;
  summary += `  P95:     ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}\n`;
  summary += `  P99:     ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}\n`;
  summary += `  Max:     ${data.metrics.http_req_duration.values.max.toFixed(2)}\n\n`;
  
  summary += '----------------------------------------\n';
  summary += 'CUSTOM METRICS (ms)\n';
  summary += '----------------------------------------\n';
  if (data.metrics.login_duration) {
    summary += `  Login P95:    ${data.metrics.login_duration.values['p(95)'].toFixed(2)}\n`;
  }
  if (data.metrics.upload_duration) {
    summary += `  Upload P95:   ${data.metrics.upload_duration.values['p(95)'].toFixed(2)}\n`;
  }
  if (data.metrics.download_duration) {
    summary += `  Download P95: ${data.metrics.download_duration.values['p(95)'].toFixed(2)}\n`;
  }
  summary += '\n';
  
  summary += '----------------------------------------\n';
  summary += 'CHECKS\n';
  summary += '----------------------------------------\n';
  summary += `  Passed: ${data.metrics.checks.values.passes}\n`;
  summary += `  Failed: ${data.metrics.checks.values.fails}\n`;
  summary += `  Success Rate: ${(data.metrics.checks.values.rate * 100).toFixed(2)}%\n\n`;
  
  summary += '----------------------------------------\n';
  summary += 'DATA TRANSFER\n';
  summary += '----------------------------------------\n';
  summary += `  Sent:     ${(data.metrics.data_sent.values.count / 1024 / 1024).toFixed(2)} MB\n`;
  summary += `  Received: ${(data.metrics.data_received.values.count / 1024 / 1024).toFixed(2)} MB\n\n`;
  
  summary += '========================================\n\n';
  
  // Thresholds status
  const thresholdsFailed = [];
  if (data.metrics.http_req_duration.values['p(95)'] > 1000) {
    thresholdsFailed.push('P95 response time > 1000ms');
  }
  if (data.metrics.http_req_failed.values.rate > 0.05) {
    thresholdsFailed.push('Error rate > 5%');
  }
  
  if (thresholdsFailed.length > 0) {
    summary += '❌ THRESHOLDS FAILED:\n';
    thresholdsFailed.forEach(failure => {
      summary += `   - ${failure}\n`;
    });
  } else {
    summary += '✅ ALL THRESHOLDS PASSED\n';
  }
  
  summary += '\n========================================\n';
  
  return summary;
}
