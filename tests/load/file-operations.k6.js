import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BASE_URL, TEST_USER, STAGED_OPTIONS } from './config.js';

// Custom metrics
const fileUploadSuccessRate = new Rate('file_upload_success_rate');
const fileDownloadSuccessRate = new Rate('file_download_success_rate');
const uploadDuration = new Trend('upload_duration');
const downloadDuration = new Trend('download_duration');
const totalFilesUploaded = new Counter('total_files_uploaded');

export const options = STAGED_OPTIONS;

let accessToken = null;
let uploadedFileIds = [];

export function setup() {
  // Login once to get token
  const loginPayload = JSON.stringify({
    email: TEST_USER.email,
    password: TEST_USER.password,
  });

  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  if (loginRes.status === 200) {
    const tokens = JSON.parse(loginRes.body);
    return { accessToken: tokens.accessToken };
  }

  console.error('Setup failed: Could not login');
  return { accessToken: null };
}

export default function (data) {
  if (!data.accessToken) {
    console.error('No access token available');
    return;
  }

  const headers = {
    Authorization: `Bearer ${data.accessToken}`,
  };

  // Test 1: List files
  const listRes = http.get(`${BASE_URL}/api/v1/files`, {
    headers: headers,
    tags: { name: 'ListFiles' },
  });

  check(listRes, {
    'list files status is 200': (r) => r.status === 200,
    'list files returns array': (r) => Array.isArray(JSON.parse(r.body)),
  });

  // Test 2: Upload a small file
  const fileContent = generateRandomContent(1024); // 1KB file
  const formData = {
    file: http.file(fileContent, `test-file-${__VU}-${__ITER}.txt`, 'text/plain'),
    folderId: 'null',
  };

  const uploadRes = http.post(`${BASE_URL}/api/v1/files/upload`, formData, {
    headers: {
      ...headers,
    },
    tags: { name: 'UploadFile' },
  });

  const uploadSuccess = check(uploadRes, {
    'upload status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'upload returns file metadata': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.id !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  fileUploadSuccessRate.add(uploadSuccess);
  uploadDuration.add(uploadRes.timings.duration);

  if (uploadSuccess) {
    totalFilesUploaded.add(1);
    const uploadedFile = JSON.parse(uploadRes.body);

    // Test 3: Download the uploaded file
    const downloadRes = http.get(`${BASE_URL}/api/v1/files/${uploadedFile.id}/download`, {
      headers: headers,
      tags: { name: 'DownloadFile' },
    });

    const downloadSuccess = check(downloadRes, {
      'download status is 200': (r) => r.status === 200,
      'download returns file content': (r) => r.body.length > 0,
    });

    fileDownloadSuccessRate.add(downloadSuccess);
    downloadDuration.add(downloadRes.timings.duration);

    // Test 4: Get file metadata
    const metadataRes = http.get(`${BASE_URL}/api/v1/files/${uploadedFile.id}`, {
      headers: headers,
      tags: { name: 'GetFileMetadata' },
    });

    check(metadataRes, {
      'metadata status is 200': (r) => r.status === 200,
      'metadata has correct filename': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.fileName.startsWith('test-file');
        } catch (e) {
          return false;
        }
      },
    });

    // Clean up: Delete the file
    if (__ITER % 10 === 0) {
      // Delete every 10th iteration to avoid filling storage
      http.del(`${BASE_URL}/api/v1/files/${uploadedFile.id}`, null, {
        headers: headers,
        tags: { name: 'DeleteFile' },
      });
    }
  }

  sleep(1);
}

function generateRandomContent(size) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  for (let i = 0; i < size; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

export function handleSummary(data) {
  return {
    'file-operations-summary.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data),
  };
}

function textSummary(data) {
  let summary = '\n========== FILE OPERATIONS LOAD TEST SUMMARY ==========\n\n';
  
  summary += `Total VUs: ${data.metrics.vus.values.max}\n`;
  summary += `Total Requests: ${data.metrics.http_reqs.values.count}\n`;
  summary += `Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s\n`;
  summary += `Failed Requests: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n\n`;

  summary += `Response Times:\n`;
  summary += `  avg: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
  summary += `  p95: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
  summary += `  p99: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n\n`;

  summary += `File Operations:\n`;
  summary += `  Upload Success Rate: ${(data.metrics.file_upload_success_rate.values.rate * 100).toFixed(2)}%\n`;
  summary += `  Download Success Rate: ${(data.metrics.file_download_success_rate.values.rate * 100).toFixed(2)}%\n`;
  summary += `  Avg Upload Duration: ${data.metrics.upload_duration.values.avg.toFixed(2)}ms\n`;
  summary += `  Avg Download Duration: ${data.metrics.download_duration.values.avg.toFixed(2)}ms\n`;
  summary += `  Total Files Uploaded: ${data.metrics.total_files_uploaded.values.count}\n\n`;

  summary += '======================================================\n';
  
  return summary;
}
