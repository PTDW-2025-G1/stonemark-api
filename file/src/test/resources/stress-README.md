# Stress Tests

## Quick Start

```bash
# Install k6 (once)
winget install k6 --source winget

# Smoke test — validates upload pipeline works
k6 run stress-smoke.js

# Optional: override server URL
k6 run -e BASE_URL=http://my-server:8080 stress-smoke.js
```

## File Summary

| File | VUs | Duration | Purpose |
|---|---|---|---|
| `stress-smoke.js` | 1 | ~1s | Fast sanity check. 5 uploads. Run after every deploy. |
| `stress-load.js` | 20 → 50 | 3m30s | Sustained throughput. Find connection pool or temp-disk saturation. |
| `stress-spike.js` | 50 → 200 | 3m | Breaking point. Requires server tuning. |

## What They Test

Each test sends valid JPEG/PNG multipart uploads to `POST /api/v1/public/media` and checks `HTTP 201`. No processing/thumbnail polling — just the upload acceptance pipeline (Tika validation, size check, storage write, DB insert).

## Server Tuning for Load/Spike

Start the server with these overrides if running load or spike tests:

```bash
--upload.rate-limit.enabled=false
--spring.datasource.hikari.maximum-pool-size=50
--server.tomcat.threads.max=400
```

Smoke test works with zero config changes.

## Thresholds

| Metric | Smoke | Load | Spike |
|---|---|---|---|
| `upload_errors` rate | < 1% | < 2% | < 5% |
| `upload_duration_ms` p95 | < 5s | < 10s | < 15s |

## Notes

- Payloads embed real JPEG/PNG magic bytes via `Uint8Array` — Tika passes MIME validation.
- Rate limiting must be disabled via `upload.rate-limit.enabled=false` for load/spike.
- Processing (Thumbnailator WebP variants) runs async and is not measured here.
