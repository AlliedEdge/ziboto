import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { BASE_URL, STAGED_OPTIONS } from './config.js';

export const options = STAGED_OPTIONS;

// Simulated user data
const users = new SharedArray('users', function () {
  const userList = [];
  for (let i = 1; i <= 100; i++) {
    userList.push({
      email: `testuser${i}@ziboto.com`,
      password: 'Test123!',
      fullName: `Test User ${i}`,
    });
  }
  return userList;
});

export function setup() {
  console.log(`Starting full scenario test with ${users.length} test users`);
  return { users };
}

export default function (data) {
  // Select a user for this VU
  const user = data.users[__VU % data.users.length];
  let accessToken = null;

  group('Authentication Flow', function () {
    // Register or login
    const loginPayload = JSON.stringify({
      email: user.email,
      password: user.password,
    });

    const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'Login' },
    });

    if (loginRes.status === 401 || loginRes.status === 404) {
      // User doesn't exist, register
      const registerPayload = JSON.stringify({
        email: user.email,
        password: user.password,
        fullName: user.fullName,
      });

      const registerRes = http.post(`${BASE_URL}/api/v1/auth/register`, registerPayload, {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'Register' },
      });

      check(registerRes, {
        'registration successful': (r) => r.status === 200 || r.status === 201,
      });

      // Try login again
      const retryLoginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, {
        headers: { 'Content-Type': 'application/json' },
      });

      if (retryLoginRes.status === 200) {
        accessToken = JSON.parse(retryLoginRes.body).accessToken;
      }
    } else if (loginRes.status === 200) {
      accessToken = JSON.parse(loginRes.body).accessToken;
    }

    check(loginRes, {
      'login successful': (r) => r.status === 200 || accessToken !== null,
    });
  });

  if (!accessToken) {
    console.error(`Failed to authenticate user ${user.email}`);
    return;
  }

  const headers = {
    Authorization: `Bearer ${accessToken}`,
  };

  group('User Profile Operations', function () {
    const profileRes = http.get(`${BASE_URL}/api/v1/users/profile`, {
      headers: headers,
      tags: { name: 'GetProfile' },
    });

    check(profileRes, {
      'get profile successful': (r) => r.status === 200,
    });
  });

  group('File Operations', function () {
    // List files
    const listRes = http.get(`${BASE_URL}/api/v1/files`, {
      headers: headers,
      tags: { name: 'ListFiles' },
    });

    check(listRes, {
      'list files successful': (r) => r.status === 200,
    });

    // Upload a file (20% of users)
    if (Math.random() < 0.2) {
      const fileContent = `Test content for ${user.email} - iteration ${__ITER}`;
      const formData = {
        file: http.file(fileContent, `test-${__VU}-${__ITER}.txt`, 'text/plain'),
      };

      const uploadRes = http.post(`${BASE_URL}/api/v1/files/upload`, formData, {
        headers: headers,
        tags: { name: 'UploadFile' },
      });

      check(uploadRes, {
        'upload successful': (r) => r.status === 200 || r.status === 201,
      });
    }
  });

  group('Storage Analytics', function () {
    const analyticsRes = http.get(`${BASE_URL}/api/v1/analytics/storage`, {
      headers: headers,
      tags: { name: 'GetStorageAnalytics' },
    });

    check(analyticsRes, {
      'analytics successful': (r) => r.status === 200,
    });
  });

  group('Activity Feed', function () {
    const activityRes = http.get(`${BASE_URL}/api/v1/activity`, {
      headers: headers,
      tags: { name: 'GetActivity' },
    });

    check(activityRes, {
      'activity feed successful': (r) => r.status === 200,
    });
  });

  sleep(1 + Math.random() * 2); // Random sleep between 1-3 seconds
}

export function handleSummary(data) {
  return {
    'full-scenario-summary.json': JSON.stringify(data, null, 2),
    'full-scenario-summary.html': htmlReport(data),
  };
}

function htmlReport(data) {
  const metrics = data.metrics;
  return `
<!DOCTYPE html>
<html>
<head>
  <title>Ziboto Load Test Report</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
    .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; }
    h1 { color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }
    h2 { color: #666; margin-top: 30px; }
    .metric { display: flex; justify-content: space-between; padding: 10px; border-bottom: 1px solid #eee; }
    .metric-name { font-weight: bold; }
    .metric-value { color: #4CAF50; }
    .success { color: #4CAF50; }
    .warning { color: #FF9800; }
    .error { color: #F44336; }
    .summary-box { background: #f9f9f9; padding: 15px; border-radius: 5px; margin: 15px 0; }
  </style>
</head>
<body>
  <div class="container">
    <h1>Ziboto Full Scenario Load Test Report</h1>
    
    <div class="summary-box">
      <h2>Test Summary</h2>
      <div class="metric">
        <span class="metric-name">Maximum VUs:</span>
        <span class="metric-value">${metrics.vus.values.max}</span>
      </div>
      <div class="metric">
        <span class="metric-name">Total Requests:</span>
        <span class="metric-value">${metrics.http_reqs.values.count}</span>
      </div>
      <div class="metric">
        <span class="metric-name">Request Rate:</span>
        <span class="metric-value">${metrics.http_reqs.values.rate.toFixed(2)} req/s</span>
      </div>
      <div class="metric">
        <span class="metric-name">Failed Requests:</span>
        <span class="${metrics.http_req_failed.values.rate < 0.01 ? 'success' : 'error'}">
          ${(metrics.http_req_failed.values.rate * 100).toFixed(2)}%
        </span>
      </div>
    </div>

    <div class="summary-box">
      <h2>Response Times</h2>
      <div class="metric">
        <span class="metric-name">Average:</span>
        <span class="metric-value">${metrics.http_req_duration.values.avg.toFixed(2)}ms</span>
      </div>
      <div class="metric">
        <span class="metric-name">Minimum:</span>
        <span class="metric-value">${metrics.http_req_duration.values.min.toFixed(2)}ms</span>
      </div>
      <div class="metric">
        <span class="metric-name">Maximum:</span>
        <span class="metric-value">${metrics.http_req_duration.values.max.toFixed(2)}ms</span>
      </div>
      <div class="metric">
        <span class="metric-name">95th Percentile:</span>
        <span class="${metrics.http_req_duration.values['p(95)'] < 500 ? 'success' : 'warning'}">
          ${metrics.http_req_duration.values['p(95)'].toFixed(2)}ms
        </span>
      </div>
      <div class="metric">
        <span class="metric-name">99th Percentile:</span>
        <span class="${metrics.http_req_duration.values['p(99)'] < 1000 ? 'success' : 'warning'}">
          ${metrics.http_req_duration.values['p(99)'].toFixed(2)}ms
        </span>
      </div>
    </div>

    <div class="summary-box">
      <h2>Test Passed: <span class="${metrics.http_req_failed.values.rate < 0.01 && metrics.http_req_duration.values['p(95)'] < 500 ? 'success' : 'error'}">${metrics.http_req_failed.values.rate < 0.01 && metrics.http_req_duration.values['p(95)'] < 500 ? 'YES' : 'NO'}</span></h2>
      <p>Criteria: <1% error rate, p95 <500ms</p>
    </div>
  </div>
</body>
</html>
  `;
}
