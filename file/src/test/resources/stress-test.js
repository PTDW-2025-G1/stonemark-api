import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const uploadDuration = new Trend('upload_duration_ms');
const processingLatency = new Trend('processing_ready_latency_ms');
const uploadErrors = new Rate('upload_errors');
const processingTimeouts = new Rate('processing_timeouts');

const FILES = new SharedArray('test-images', function () {
  return [
    { name: 'small.jpg',  size: 128 * 1024,       type: 'image/jpeg' },
    { name: 'medium.jpg', size: 512 * 1024,       type: 'image/jpeg' },
    { name: 'large.jpg',  size: 1 * 1024 * 1024,  type: 'image/jpeg' },
    { name: 'xlarge.png', size: 4 * 1024 * 1024,  type: 'image/png'  },
    { name: 'max.jpg',    size: 10 * 1024 * 1024, type: 'image/jpeg' },
  ];
});

function generatePayload(sizeBytes, mimeType) {
  const boundary = '----WebKitFormBoundary' + Math.random().toString(36).slice(2);
  const header = `--${boundary}\r\n` +
    `Content-Disposition: form-data; name="file"; filename="test${Math.random()}.${mimeType.split('/')[1]}"\r\n` +
    `Content-Type: ${mimeType}\r\n\r\n`;
  const footer = `\r\n--${boundary}--\r\n`;
  const bodySize = sizeBytes - header.length - footer.length;
  const body = 'X'.repeat(Math.max(0, bodySize));
  return {
    body: header + body + footer,
    headers: { 'Content-Type': `multipart/form-data; boundary=${boundary}` },
  };
}

export const options = {
  stages: [
    { target: 200,  duration: '30s' },
    { target: 500,  duration: '30s' },
    { target: 1000, duration: '1m'  },
    { target: 1000, duration: '2m'  },
    { target: 0,    duration: '30s' },
  ],
  thresholds: {
    upload_errors:        ['rate<0.01'],
    upload_duration_ms:   ['p(95)<5000'],
    processing_timeouts:  ['rate<0.02'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PROCESSING_TIMEOUT_SEC = parseInt(__ENV.PROCESSING_TIMEOUT || '30');

export default function () {
  const fileSpec = randomItem(FILES);
  const payload = generatePayload(fileSpec.size, fileSpec.type);

  const uploadStart = Date.now();
  const uploadResp = http.post(`${BASE_URL}/api/v1/public/media`, payload.body, {
    headers: payload.headers,
    timeout: '30s',
  });
  const uploadElapsed = Date.now() - uploadStart;
  uploadDuration.add(uploadElapsed);

  const ok = check(uploadResp, {
    'upload status 201': (r) => r.status === 201,
    'upload has id': (r) => r.json('id') !== undefined,
    'upload has status': (r) => r.json('status') !== undefined,
  });

  if (!ok) {
    uploadErrors.add(1);
    return;
  }

  const mediaId = uploadResp.json('id');

  const deadline = Date.now() + PROCESSING_TIMEOUT_SEC * 1000;
  let ready = false;
  while (Date.now() < deadline) {
    const statusResp = http.get(`${BASE_URL}/api/v1/public/media/${mediaId}`, {
      timeout: '5s',
    });
    if (statusResp.status === 200) {
      const status = statusResp.json('status');
      if (status === 'READY' || status === 'FAILED') {
        ready = true;
        processingLatency.add(Date.now() - uploadStart);
        check(statusResp, {
          'processing completed': () => status === 'READY',
        });
        break;
      }
      if (status === 'FAILED') {
        processingTimeouts.add(1);
        break;
      }
    }
    sleep(0.5);
  }

  if (!ready) {
    processingTimeouts.add(1);
  }
}

export function handleSummary(data) {
  return {
    'stdout': JSON.stringify({
      upload_p95_ms:             data.metrics.upload_duration_ms?.values?.['p(95)'],
      upload_error_rate:         data.metrics.upload_errors?.values?.rate,
      processing_timeout_rate:   data.metrics.processing_timeouts?.values?.rate,
      processing_latency_p95_ms: data.metrics.processing_ready_latency_ms?.values?.['p(95)'],
      total_uploads:             data.metrics.http_reqs?.values?.count,
    }, null, 2),
  };
}
