import http from 'k6/http';

const JPEG_MAGIC = new Uint8Array([0xFF, 0xD8, 0xFF]);
const body = new Uint8Array(128 * 1024);
body.set(JPEG_MAGIC, 0);

const formData = {
  file: http.file(body.buffer, 'test.jpg', 'image/jpeg'),
};

export const options = { vus: 1, iterations: 2 };

export default function () {
  const resp = http.post('http://localhost:8080/api/v1/public/media', formData, { timeout: '20s' });
  console.log('STATUS:' + resp.status);
  console.log('BODY:' + (resp.body || '(empty)').substring(0, 500));
}
