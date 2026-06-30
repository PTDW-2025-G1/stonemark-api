# Processing Module

## Overview

The `processing` module transforms raw submission evidence into ranked mark suggestions via Vision AI detection and vector similarity search. It bridges intake (submission ingestion) and review (human decision).

## Flow

```
intake publishes MarkEvidenceSubmittedEvent
  ↓
MarkEvidenceSubmittedListener (creates PENDING placeholder, dispatches async)
  ↓
ProcessingServiceImpl.processSubmission() (async thread)
  ↓
Phase A: create or reuse processing record (pessimistic lock, idempotency)
  ↓
Phase B: Vision detection → embedding → pgvector similarity search
  ↓
Phase C: persist embedding + suggestions, mark COMPLETED
  ↓
ReviewService (human decides: accept suggestion / create new mark / reject)
  ↓
Side effects: update submission status, link evidence to mark occurrence
```

## Core Components

### MarkEvidenceSubmittedListener
Receives `@ApplicationModuleListener` event. Creates a `PENDING` placeholder processing record if one doesn't exist. Re-dispatches async processing if an existing record is in `PENDING`/`FAILED` state (handles Modulith event replay). Registers `afterCommit` synchronization to dispatch async processing after the listener's transaction commits.

### ProcessingServiceImpl
Orchestrates the processing pipeline on an async thread. Three phases:

- **Phase A** (transactional): Delegates to `ProcessingPersistenceService.createOrReuseProcessingRecord()` — pessimistic lock with idempotency checks. Returns null if already `PROCESSING`/`COMPLETED`. Handles race conditions via nested `REQUIRES_NEW` transaction on `DataIntegrityViolationException`.
- **Phase B** (non-transactional): Vision availability check, semaphore-backed Vision API call, embedding normalization, pgvector similarity search via `SimilarityService`.
- **Phase C** (transactional): Persists embedding and suggestions, marks `COMPLETED`. Guards against reaper reset by checking status is still `PROCESSING`.

### ProcessingPersistenceService
Transactional wrapper for all DB state transitions: `createOrReuseProcessingRecord`, `setPending`, `setFailed`, `finalizeSuccess`. Guards `setPending`/`setFailed` against reverting `COMPLETED`/`REVIEWED` records.

### SimilarityService
Simplified similarity pipeline:
1. Convert embedding to pgvector literal
2. Query top-K similar evidence via `MarkEvidenceQueryService.findTopKSimilar()`
3. Filter NaN/Infinity values
4. Fetch mark associations via `findMarksByEvidenceIds()`
5. Group by mark ID, sum confidence
6. Sort by confidence descending, create `MarkSuggestion` entities

### AsyncProcessingService
Thin `@Async` wrapper that delegates to `ProcessingServiceImpl.processSubmission()` on a dedicated thread pool.

### ProcessingRetryScheduler
Periodic (`processing.retry.interval`, default 60s): collects `PENDING`/`FAILED` records with elapsed exponential backoff. Dispatches in configurable batches with pauses. Also collects orphaned `RECEIVED` submissions without processing records. Skips when Vision service is unavailable.

### ProcessingStuckReaper
Periodic (`processing.reaper.interval`, default 1h): resets records stuck in `PROCESSING` for >30min back to `PENDING`. Uses `processingStartedAt` fence — only resets records where no progress was ever persisted (`updatedAt == processingStartedAt`).

## State Machine

```
RECEIVED (intake)  →  PENDING (placeholder)  →  PROCESSING (acquired)
  ↓                                                 ↓
  (retry)                                           FAILED (retryable/permanent)
                                                    ↓
                                                  COMPLETED (suggestions ready)
                                                    ↓
                                                  REVIEWED (after human decision)
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `processing.vision.max-concurrency` | 4 | Max concurrent Vision API calls |
| `processing.vision.acquire-timeout-ms` | 10000 | Semaphore acquire timeout |
| `processing.similarity.maxK` | 20 | Max candidates returned |
| `processing.similarity.minScore` | 0.5 | Minimum similarity threshold (maxDistance = 1 - minScore) |
| `processing.retry.base-delay-ms` | 60000 | Base backoff for retry |
| `processing.retry.max-delay-ms` | 1800000 | Max backoff (30 min) |
| `processing.retry.batch-size` | 5 | Retry/orphan batch size |
| `processing.retry.interval` | 60000 | Retry scheduler interval |
| `processing.reaper.max-stuck-minutes` | 30 | Stuck PROCESSING threshold |
| `processing.reaper.interval` | 3600000 | Reaper interval (1 hour) |

## Review Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `review.allow-empty-review` | false | Allow review with no suggestions |
| `review.new-mark.max-suggestion-confidence` | 0.5 | Threshold forcing existing mark review before new mark creation |
