import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const uploadDuration = new Trend('upload_duration_ms');
const uploadErrors = new Rate('upload_errors');

const HEADERS = {
  jpeg: new Uint8Array([0xFF, 0xD8, 0xFF]),
  png:  new Uint8Array([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]),
};

const SIZES = [
  { type: 'jpeg', mime: 'image/jpeg', size: 128 * 1024       },
  { type: 'jpeg', mime: 'image/jpeg', size: 512 * 1024       },
  { type: 'jpeg', mime: 'image/jpeg', size: 1 * 1024 * 1024  },
  { type: 'png',  mime: 'image/png',  size: 2 * 1024 * 1024  },
];

function makePayload(s) {
  const body = new Uint8Array(s.size);
  body.set(HEADERS[s.type], 0);
  return { file: http.file(body.buffer, 'test.' + s.type, s.mime) };
}

export const options = {
  scenarios: {
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { target: 20, duration: '30s' },
        { target: 50, duration: '30s' },
        { target: 50, duration: '2m'  },
        { target: 0,  duration: '30s' },
      ],
    },
  },
  thresholds: {
    upload_errors:      ['rate<0.02'],
    upload_duration_ms: ['p(95)<10000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const spec = randomItem(SIZES);
  const start = Date.now();
  const resp = http.post(`${BASE_URL}/api/v1/public/media`, makePayload(spec), {
    timeout: '30s',
  });
  uploadDuration.add(Date.now() - start);
  check(resp, { 'status 201': (r) => r.status === 201 }) || uploadErrors.add(1);
}
