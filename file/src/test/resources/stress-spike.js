import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const uploadDuration = new Trend('upload_duration_ms');
const processingLatency = new Trend('processing_ready_latency_ms');
const uploadErrors = new Rate('upload_errors');
const processingTimeouts = new Rate('processing_timeouts');

const FILES = new SharedArray('test-images', function () {
  return [
    { name: '128k.jpg',  size: 128 * 1024,        type: 'image/jpeg' },
    { name: '512k.jpg',  size: 512 * 1024,        type: 'image/jpeg' },
    { name: '1m.jpg',    size: 1 * 1024 * 1024,   type: 'image/jpeg' },
    { name: '4m.png',    size: 4 * 1024 * 1024,   type: 'image/png'  },
    { name: '10m.jpg',   size: 10 * 1024 * 1024,  type: 'image/jpeg' },
  ];
});

function generatePayload(sizeBytes, mimeType) {
  const boundary = '----FormBoundary' + Math.random().toString(36).slice(2);
  const header = `--${boundary}\r\n` +
    `Content-Disposition: form-data; name="file"; filename="t.jpg"\r\n` +
    `Content-Type: ${mimeType}\r\n\r\n`;
  const footer = `\r\n--${boundary}--\r\n`;
  const body = 'X'.repeat(Math.max(0, sizeBytes - header.length - footer.length));
  return {
    body: header + body + footer,
    headers: { 'Content-Type': `multipart/form-data; boundary=${boundary}` },
  };
}

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { target: 50,   duration: '30s' },
        { target: 100,  duration: '30s' },
        { target: 200,  duration: '30s' },
        { target: 200,  duration: '1m'  },
        { target: 0,    duration: '30s' },
      ],
    },
  },
  thresholds: {
    upload_errors:       ['rate<0.05'],
    upload_duration_ms:  ['p(95)<15000'],
    processing_timeouts: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PROCESSING_TIMEOUT_SEC = parseInt(__ENV.PROCESSING_TIMEOUT || '45');

export default function () {
  const fileSpec = randomItem(FILES);
  const payload = generatePayload(fileSpec.size, fileSpec.type);

  const start = Date.now();
  const resp = http.post(`${BASE_URL}/api/v1/public/media`, payload.body, {
    headers: payload.headers,
    timeout: '45s',
  });
  uploadDuration.add(Date.now() - start);

  const ok = check(resp, {
    'status 201': (r) => r.status === 201,
    'has id':     (r) => r.json('id') !== undefined,
  });

  if (!ok) { uploadErrors.add(1); return; }

  const mediaId = resp.json('id');
  const deadline = Date.now() + PROCESSING_TIMEOUT_SEC * 1000;
  let ready = false;
  while (Date.now() < deadline) {
    const s = http.get(`${BASE_URL}/api/v1/public/media/${mediaId}`, { timeout: '5s' });
    if (s.status === 200) {
      const st = s.json('status');
      if (st === 'READY' || st === 'FAILED') {
        ready = true;
        processingLatency.add(Date.now() - start);
        check(s, { 'is ready': () => st === 'READY' });
        break;
      }
    }
    sleep(0.5);
  }
  if (!ready) processingTimeouts.add(1);
}

export function handleSummary(data) {
  return {
    stdout: JSON.stringify({
      upload_p95_ms:             data.metrics.upload_duration_ms?.values?.['p(95)'],
      upload_error_rate:         data.metrics.upload_errors?.values?.rate,
      processing_timeout_rate:   data.metrics.processing_timeouts?.values?.rate,
      processing_latency_p95_ms: data.metrics.processing_ready_latency_ms?.values?.['p(95)'],
      total:                     data.metrics.http_reqs?.values?.count,
    }, null, 2),
  };
}
