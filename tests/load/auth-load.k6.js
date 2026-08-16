import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, TEST_USER, STAGED_OPTIONS } from './config.js';

// Custom metrics
const loginSuccessRate = new Rate('login_success_rate');
const loginDuration = new Trend('login_duration');
const refreshSuccessRate = new Rate('refresh_success_rate');

export const options = STAGED_OPTIONS;

export function setup() {
  // Register test user if not exists
  const registerPayload = JSON.stringify({
    email: TEST_USER.email,
    password: TEST_USER.password,
    fullName: 'Load Test User',
  });

  const registerRes = http.post(`${BASE_URL}/api/v1/auth/register`, registerPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  console.log(`Setup: User registration status ${registerRes.status}`);
  return { testUser: TEST_USER };
}

export default function (data) {
  // Login
  const loginPayload = JSON.stringify({
    email: data.testUser.email,
    password: data.testUser.password,
  });

  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'Login' },
  });

  const loginSuccess = check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'login returns access token': (r) => JSON.parse(r.body).accessToken !== undefined,
    'login returns refresh token': (r) => JSON.parse(r.body).refreshToken !== undefined,
  });

  loginSuccessRate.add(loginSuccess);
  loginDuration.add(loginRes.timings.duration);

  if (loginSuccess) {
    const tokens = JSON.parse(loginRes.body);

    // Get user profile
    const profileRes = http.get(`${BASE_URL}/api/v1/users/profile`, {
      headers: {
        Authorization: `Bearer ${tokens.accessToken}`,
      },
      tags: { name: 'GetProfile' },
    });

    check(profileRes, {
      'profile status is 200': (r) => r.status === 200,
      'profile returns user data': (r) => JSON.parse(r.body).email === data.testUser.email,
    });

    // Test token refresh
    const refreshPayload = JSON.stringify({
      refreshToken: tokens.refreshToken,
    });

    const refreshRes = http.post(`${BASE_URL}/api/v1/auth/refresh`, refreshPayload, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'RefreshToken' },
    });

    const refreshSuccess = check(refreshRes, {
      'refresh status is 200': (r) => r.status === 200,
      'refresh returns new access token': (r) => JSON.parse(r.body).accessToken !== undefined,
    });

    refreshSuccessRate.add(refreshSuccess);
  }

  sleep(1);
}

export function handleSummary(data) {
  return {
    'auth-load-summary.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: '  ', enableColors: true }),
  };
}

function textSummary(data, opts) {
  const indent = opts.indent || '';
  const enableColors = opts.enableColors || false;

  let summary = `\n${indent}========== AUTH LOAD TEST SUMMARY ==========\n\n`;
  
  summary += `${indent}Total VUs: ${data.metrics.vus.values.max}\n`;
  summary += `${indent}Total Requests: ${data.metrics.http_reqs.values.count}\n`;
  summary += `${indent}Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s\n`;
  summary += `${indent}Failed Requests: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n\n`;

  summary += `${indent}Response Times:\n`;
  summary += `${indent}  avg: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
  summary += `${indent}  min: ${data.metrics.http_req_duration.values.min.toFixed(2)}ms\n`;
  summary += `${indent}  max: ${data.metrics.http_req_duration.values.max.toFixed(2)}ms\n`;
  summary += `${indent}  p95: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
  summary += `${indent}  p99: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n\n`;

  summary += `${indent}Custom Metrics:\n`;
  summary += `${indent}  Login Success Rate: ${(data.metrics.login_success_rate.values.rate * 100).toFixed(2)}%\n`;
  summary += `${indent}  Refresh Success Rate: ${(data.metrics.refresh_success_rate.values.rate * 100).toFixed(2)}%\n`;
  summary += `${indent}  Avg Login Duration: ${data.metrics.login_duration.values.avg.toFixed(2)}ms\n\n`;

  summary += `${indent}==========================================\n`;
  
  return summary;
}
