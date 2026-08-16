// k6 Load Testing Configuration
export const BASE_URL = __ENV.API_URL || 'http://localhost:8080';
export const VU_TARGET = parseInt(__ENV.VU_TARGET || '1000');
export const RAMP_UP_DURATION = __ENV.RAMP_UP_DURATION || '5m';
export const HOLD_DURATION = __ENV.HOLD_DURATION || '10m';
export const RAMP_DOWN_DURATION = __ENV.RAMP_DOWN_DURATION || '2m';

// Test user credentials
export const TEST_USER = {
  email: 'loadtest@ziboto.com',
  password: 'LoadTest123!',
};

// Thresholds for success criteria
export const THRESHOLDS = {
  http_req_failed: ['rate<0.01'], // Less than 1% error rate
  http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% < 500ms, 99% < 1000ms
  http_req_duration_api: ['p(95)<300', 'p(99)<600'], // API endpoints even faster
  http_reqs: ['rate>100'], // At least 100 requests per second
};

// Options for staged load test
export const STAGED_OPTIONS = {
  stages: [
    { duration: RAMP_UP_DURATION, target: VU_TARGET }, // Ramp up to target VUs
    { duration: HOLD_DURATION, target: VU_TARGET }, // Hold at target
    { duration: RAMP_DOWN_DURATION, target: 0 }, // Ramp down
  ],
  thresholds: THRESHOLDS,
};

// Options for spike test
export const SPIKE_OPTIONS = {
  stages: [
    { duration: '1m', target: 100 }, // Warm up
    { duration: '30s', target: VU_TARGET * 2 }, // Spike to 2x target
    { duration: '1m', target: 100 }, // Cool down
    { duration: '1m', target: 0 }, // Finish
  ],
  thresholds: {
    ...THRESHOLDS,
    http_req_failed: ['rate<0.05'], // Allow 5% error during spike
  },
};

// Options for stress test
export const STRESS_OPTIONS = {
  stages: [
    { duration: '2m', target: 100 },
    { duration: '3m', target: 500 },
    { duration: '3m', target: 1000 },
    { duration: '3m', target: 1500 },
    { duration: '3m', target: 2000 }, // Find breaking point
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.10'], // Allow 10% error during stress
    http_req_duration: ['p(95)<2000'], // More relaxed during stress
  },
};
