# StoneMark API — Codebase Analysis

## Project Purpose

**StoneMark API** — a modular REST backend for digital preservation and analysis of **masons' marks** (historical symbols carved by stonemasons to identify work, provenance, or crew).

The core domain pipeline:

```
Intake (submit photos) → Processing (AI vision + similarity matching) → Review (human moderation) → Persistence (mark/monument entities)
```

### Supporting capabilities
- Digital asset management (MinIO S3-compatible storage)
- Geospatial search (PostGIS + pgvector)
- Chatbot integrations (Telegram + WhatsApp)
- RBAC with OAuth2 (self-issued + Google)
- Reporting and export

---

## Architecture Overview

| Layer | Modules | Purpose |
|-------|---------|---------|
| **Foundation** | `shared-core`, `shared-web`, `shared-infra`, `file-api`, `mark-api`, `user-api` | Shared enums, JPA bases, SPIs |
| **Domain** | `mark`, `monument`, `territory`, `user`, `file`, `support`, `bookmark` | Core entities and business logic |
| **Pipeline** | `intake`, `processing`, `review`, `verification` | Submission → similarity → moderation workflow |
| **Integration** | `chatbot`, `vision`, `notification` | External interfaces |
| **Entry** | `boot` | Composition root, security, config |

**Pattern**: Modular monolith with hexagonal architecture (SPI ports in `-api` modules, adapters in implementations). Event-driven cross-module communication via Spring `ApplicationEvent`.

**Stack**: Java 25, Spring Boot 4.0.4, PostgreSQL 18 + PostGIS + pgvector, MinIO, Flyway, Bucket4j, Thumbnailator.

---

## Over-Engineering & Unnecessary Complexity

### 🔴 Critical — Clear over-engineering with no justification

#### 1. Processing aggregation pipeline: 9 classes for 4 logical stages

**Location**: `processing/src/main/java/.../services/similarity/aggregation/`

9 files for a pipeline with 4 stages (normalize → deduplicate → resolve fan-out → score):

| File | Role |
|------|------|
| `CandidateNormalizationStage.java` | Clamps similarity to [0,1], filters NaN |
| `CandidateDeduplicationStage.java` | Removes duplicate tuples |
| `FanOutResolver.java` | Computes per-evidence mark count |
| `MarkScoringStage.java` | Applies decay, rank weighting, fan-out scaling |
| `AggregationPipeline.java` | Orchestrates the 4 stages (hard-coded `new`) |
| `ScoreCalculator.java` | `@Component` that only constructs `AggregationPipeline` and delegates |
| `AggregationResultBuilder.java` | Converts raw scores to quantized `MarkScore` list |
| `CandidateGrouper.java` | Groups `CandidateEvidence` by mark ID |
| `MarkAggregator.java` | Entry point: grouper → calculator → result builder |

**Problem**: `ScoreCalculator` is a useless delegation layer. Each stage being its own class adds no value — they could be private methods in a single `MarkScoringService`. The pipeline hard-codes stage construction (`new`) rather than injecting, making the `@Component` layer decorative.

#### 2. Kahan summation for masonry marks scoring

**Location**: `processing/src/main/java/.../utils/KahanAccumulator.java`, `processing/src/main/java/.../models/KahanState.java`

Implements Neumaier's compensated summation — an algorithm designed for extreme floating-point precision requirements (orbital mechanics, climate modelling, financial calculations with billions of operations).

**Problem**: For ranking similarity scores of stone carving photos, `double` provides ~15-17 decimal digits of precision. Kahan summation is irrelevant here. The concept is also split across two classes in different packages (`models/` and `utils/`) when it could be a single utility.

#### 3. Five Policy classes instead of one `@ConfigurationProperties`

**Location**: `processing/src/main/java/.../config/policies/`

| File | Fields | Lines |
|------|--------|-------|
| `EmbeddingPolicy.java` | `dimension` | 22 |
| `ParityPolicy.java` | `async`, `tolerance`, `sampleSize` | 32 |
| `SanitizationPolicy.java` | `minSimilarity`, `maxSimilarity` | 34 |
| `ScoringPolicy.java` | `useRankWeighting`, `perMarkDecay`, `fanOutStrategy` | 39 |
| `SimilarityPolicy.java` | `parityEnabled`, `maxK`, `minScore` | 33 |

Each is a `@Component` with `@Value` injection and its own validation boilerplate.

**Problem**: ~160 lines across 5 files for 10 configuration values. A single `@ConfigurationProperties(prefix = "processing")` record with logical groupings would eliminate the repetition.

#### 4. Dead code: 4 duplicate ValidationState files

| File | Status | Content |
|------|--------|---------|
| `mark/enums/MarkValidationState.java` | **Dead** | Verbatim copy of shared `ValidationState` |
| `mark/converters/ValidationStateConverter.java` | **Dead** | Structural copy of shared converter, imports wrong enum |
| `monument/enums/MonumentValidationState.java` | **Dead** | Partial copy (`VERIFIED`, `PHANTOM`), never wired |
| `monument/converters/MonumentValidationStateConverter.java` | **Dead** | Structural copy of shared converter, imports wrong enum |

All entities (`Mark`, `MarkOccurrence`, `Monument`) correctly import `pt.estga.shared.enums.ValidationState` and `pt.estga.shared.converters.ValidationStateConverter`. These 4 files are unreferenced and should be removed.

---

### 🟡 Moderate — Unnecessary complexity, should simplify

#### 5. RejectionReviewProcessor as a full strategy class

**Location**: `review/src/main/java/.../processors/RejectionReviewProcessor.java`

```java
@Component
public class RejectionReviewProcessor implements ReviewProcessor {
    @Override
    public ReviewType getSupportedType() { return ReviewType.REJECTION; }
    @Override
    public ResolutionResult resolve(Long submissionId, DiscoveryContext context) {
        return new ResolutionResult(null, null);
    }
}
```

3 lines of real logic wrapped in a full `@Component` with interface, routing enum, and stream-filter discovery. Should be inlined in the service.

#### 6. GroupReviewProcessor duplicates DiscoveryReviewProcessor

`GroupReviewProcessor` and `DiscoveryReviewProcessor` are ~95% identical. The only difference:

| Processor | Monument creation condition |
|-----------|---------------------------|
| `DiscoveryReviewProcessor` | `monumentName() != null \|\| location() != null` |
| `GroupReviewProcessor` | `location() != null` |

Copy-paste reuse. Should be a single parameterized processor.

#### 7. Triple recovery mechanism for processing

Three overlapping scheduled mechanisms ensuring one thing (a processing record eventually completes):

| Scheduler | Interval | Purpose |
|-----------|----------|---------|
| `ProcessingOutboxPoller` | 5s | Dispatches pending outbox entries |
| `ProcessingRetryScheduler` | 60s | Retries failed/pending entries with exponential backoff |
| `ProcessingStuckReaper` | 60min | Resets records stuck in PROCESSING >30min back to PENDING |

**Problem**: The stuck reaper exists because records can remain in PROCESSING for >30 minutes, suggesting the pipeline is fragile or the vision AI is unreliable. If the outbox and retry scheduler were sufficient, the reaper would never trigger. Three mechanisms for recovery is belt-and-suspenders.

#### 8. 10 internal model classes for processing pipeline

**Location**: `processing/src/main/java/.../models/`

`AggregationKey`, `AggregationResult`, `AggregationState`, `CandidateEvidence`, `CandidateKey`, `KahanState`, `MarkScore`, `SanitizationKey`, `SanitizationResult`, `ScoringResult`

Many are 2-3 field records that intermediate between pipeline stages. The stage fragmentation drives model proliferation: each stage needs its own input/output types, creating a long chain of intermediate representations for what is ultimately a weighted sum.

#### 9. Stream-filter routing for review processors

```java
ReviewProcessor processor = processors.stream()
        .filter(p -> p.getSupportedType() == type)
        .findFirst()
        .orElseThrow(...);
```

O(n) linear scan on every review. If two processors accidentally declare the same type, the first in injection order wins silently. A `Map<ReviewType, ReviewProcessor>` at startup would be O(1) and fail-fast on duplicates.

#### 10. Processing module overall size

66 main source files, 31 test files (97 total) for a single capability: scoring similarity evidence into mark suggestions. The scoring formula is a few lines of math, but the implementation spans:

- 6 entities
- 10 models
- 4 enums
- 5 repositories + 3 projections
- 3 schedulers
- ~20 service classes
- 2 markdown specification documents

---

### 🟢 Minor — Questionable but partially justified

| Issue | Location | Assessment |
|-------|----------|------------|
| `ImportController` as REST endpoint | `territory` | GeoJSON import is a rare/one-time admin operation. Should be CLI or batch job, not a REST endpoint. |
| Bookmark sealed interface + 4 typed records | `bookmark` | Reasonable for heterogeneous target types (Long vs UUID IDs, different domains). However, elaborate for "save this ID for later". Pattern is defensible. |
| SpatialCluster → phantom Monument creation | `processing` | After group review, automatically creates Monuments and MarkOccurrences without user knowledge. Tightly couples processing with domain entity creation. |
| 22 aggregation test files | `processing` | Tests scale with source fragmentation. Simplifying the 9 pipeline classes would proportionally reduce test scaffolding. |
| Exported metrics catching exceptions silently | `review`/`ReviewExecutor` | Metrics errors caught and swallowed with empty catch block. Indicates metrics are an afterthought bolted onto the review flow. |

---

### ✅ Well-Engineered (keep as-is)

| Module | Reason |
|--------|--------|
| **Chatbot** | Extensive but justified for dual-platform Telegram + WhatsApp with conversation state machines, file handling, and auth flows. |
| **File** | Proper `FileStorageOperations` SPI with MinIO + Local implementations. Clean separation of upload, validation, variant generation, and metadata. |
| **Event-driven architecture** | Appropriate decoupling between modules. Events are well-scoped and listeners are focused. |
| **Modular monolith structure** | Clear bounded contexts. Each module has a consistent layered structure. Easy to navigate. |
| **Vision client** | Minimal, correct — 2 classes (interface + DTO). No over-abstraction. |
| **Support / Notification** | Appropriately simple CRUD + email sending. No over-engineering. |
| **Mark / Monument / Territory** | Focused domain modules with proportional complexity. |
| **Security architecture** | Proper OAuth2 setup with RBAC, action codes, and Google OAuth integration. |

---

## Summary

| Category | Modules affected | Action |
|----------|-----------------|--------|
| **Dead code** | `mark`, `monument` | Remove 4 unused ValidationState files |
| **Over-engineered** | `processing` | Collapse 9 pipeline classes → 2-3; remove Kahan summation; merge 5 policies → 1 config; reduce model proliferation |
| **Duplicated** | `review` | Merge GroupReviewProcessor into DiscoveryReviewProcessor; inline RejectionReviewProcessor |
| **Suboptimal** | `review`, `processing` | Replace stream-filter routing with Map; evaluate if triple recovery is necessary |
| **Questionable** | `territory`, `processing` | Move ImportController to CLI; consider removing automatic phantom entity creation |

The system is well-architected at the macro level (modular monolith, event-driven, hexagonal ports). The over-engineering is concentrated in the **processing module** (~50% of all issues) and manifests as abstraction fragmentation — many small classes where few would do.
