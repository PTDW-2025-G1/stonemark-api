import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const uploadDuration = new Trend('upload_duration_ms');
const processingLatency = new Trend('processing_ready_latency_ms');
const uploadErrors = new Rate('upload_errors');
const processingTimeouts = new Rate('processing_timeouts');
const rateLimited = new Rate('rate_limited');

const FILES = new SharedArray('test-images', function () {
  return [
    { name: '128k.jpg', size: 128 * 1024,       type: 'image/jpeg' },
    { name: '512k.jpg', size: 512 * 1024,       type: 'image/jpeg' },
    { name: '1m.jpg',   size: 1 * 1024 * 1024,  type: 'image/jpeg' },
    { name: '2m.png',   size: 2 * 1024 * 1024,  type: 'image/png'  },
  ];
});

function generatePayload(sizeBytes, mimeType) {
  const boundary = '----FormBoundary' + Math.random().toString(36).slice(2);
  const header = `--${boundary}\r\n` +
    `Content-Disposition: form-data; name="file"; filename="test.jpg"\r\n` +
    `Content-Type: ${mimeType}\r\n\r\n`;
  const footer = `\r\n--${boundary}--\r\n`;
  const padding = header.length + footer.length;
  const body = 'X'.repeat(Math.max(0, sizeBytes - padding));
  return {
    body: header + body + footer,
    headers: { 'Content-Type': `multipart/form-data; boundary=${boundary}` },
  };
}

export const options = {
  scenarios: {
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { target: 20,  duration: '30s' },
        { target: 50,  duration: '30s' },
        { target: 50,  duration: '2m'  },
        { target: 0,   duration: '30s' },
      ],
    },
  },
  thresholds: {
    upload_errors:       ['rate<0.02'],
    upload_duration_ms:  ['p(95)<10000'],
    processing_timeouts: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PROCESSING_TIMEOUT_SEC = parseInt(__ENV.PROCESSING_TIMEOUT || '30');

function tryJsonBody(resp) {
  try { return resp.json(); } catch (_) { return null; }
}

export default function () {
  const fileSpec = randomItem(FILES);
  const payload = generatePayload(fileSpec.size, fileSpec.type);

  const start = Date.now();
  const resp = http.post(`${BASE_URL}/api/v1/public/media`, payload.body, {
    headers: payload.headers,
    timeout: '30s',
  });
  uploadDuration.add(Date.now() - start);

  if (resp.status === 429) {
    rateLimited.add(1);
    const retryAfter = parseInt(resp.headers['Retry-After'] || '1');
    sleep(Math.max(1, retryAfter));
    return;
  }

  const json = tryJsonBody(resp);
  const ok = check(resp, {
    'status 201': (r) => r.status === 201,
    'has id':     () => json && json.id !== undefined,
  });

  if (!ok) { uploadErrors.add(1); return; }

  const mediaId = json.id;
  const deadline = Date.now() + PROCESSING_TIMEOUT_SEC * 1000;
  let ready = false;
  while (Date.now() < deadline) {
    const s = http.get(`${BASE_URL}/api/v1/public/media/${mediaId}`, { timeout: '5s' });
    const sjson = tryJsonBody(s);
    if (s.status === 200 && sjson) {
      const st = sjson.status;
      if (st === 'READY' || st === 'FAILED') {
        ready = true;
        processingLatency.add(Date.now() - start);
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
      rate_limited:              data.metrics.rate_limited?.values?.rate,
      processing_timeout_rate:   data.metrics.processing_timeouts?.values?.rate,
      processing_latency_p95_ms: data.metrics.processing_ready_latency_ms?.values?.['p(95)'],
      total:                     data.metrics.http_reqs?.values?.count,
    }, null, 2),
  };
}
