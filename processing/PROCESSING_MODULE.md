# Processing Module

## Overview

The `processing` module is responsible for transforming raw evidence similarity data into ranked mark suggestions.

It implements a deterministic aggregation pipeline that:
- Sanitizes DB results
- Groups evidence per mark
- Computes weighted similarity scores
- Produces normalized rankings
- Generates final `MarkSuggestion` outputs

The system is designed for:
- Determinism
- Testability
- Config-driven scoring behavior
- Debuggable aggregation flow

---

## High-Level Pipeline


```text
DB projections
↓
CandidateSanitizer
↓
CandidateEvidence list
↓
CandidateGrouper
↓
Grouped contributions (mark → evidence list)
↓
ScoreCalculator
↓
AggregationState (raw scores + weights)
↓
AggregationResultBuilder
↓
MarkScore list (ranked)
↓
SuggestionBuilder
↓
MarkSuggestion output
```


---

## Core Design Principles

### 1. Determinism
All outputs are stable across runs:
- TreeMap ordering for mark IDs
- Explicit tie-breakers in sorting
- Stable grouping and iteration order

### 2. Separation of Concerns
Each component has a single responsibility:

- **Sanitizer** → validates and normalizes DB input
- **Grouper** → expands evidence into mark contributions
- **ScoreCalculator** → computes raw scores and weights
- **ResultBuilder** → converts scores into final ranked output
- **SuggestionBuilder** → maps scores to persistence entities

---

## Key Concepts

### CandidateEvidence
Represents a single similarity hit from DB:

- `evidenceId`
- `occurrenceId`
- `similarity`

This is the atomic unit of scoring.

---

### Fan-Out Strategy

Each evidence may contribute to multiple marks.

Two strategies exist:

- **SPLIT**
    - contribution is divided across marks
    - prevents score inflation

- **FULL**
    - full contribution applied to each mark
    - increases scoring sensitivity

Fan-out is computed dynamically from grouped evidence.

---

### Decay System

Within a single mark group:
- later evidence contributions are decayed

Formula:

decayFactor = perMarkDecay ^ positionIndex


This reduces impact of repeated lower-ranked signals.

---

### Confidence Score

Final mark score:


confidence = totalScore / weightSum


Properties:
- normalized to [0, 1]
- relative ranking metric (NOT probability)
- clamped and quantized for stability

---

## Components

### CandidateSanitizer
Responsibilities:
- Remove invalid DB rows
- Clamp similarity to configured bounds
- Track anomalies

Output:
- `List<CandidateEvidence>`
- metadata (invalid/out-of-range counts)

---

### CandidateGrouper
Responsibilities:
- Group evidence by mark ID
- Expand evidence → multiple marks
- Sort within groups by:
    1. similarity (desc)
    2. occurrenceId
    3. evidenceId

---

### ScoreCalculator
Responsibilities:
- Deduplicate contributions
- Apply decay
- Apply rank weighting
- Apply fan-out scaling
- Compute:
    - raw scores
    - weight sums
    - diagnostics counters

Output:
- `AggregationState`

---

### AggregationResultBuilder
Responsibilities:
- Convert raw scores → normalized confidence
- Sort results deterministically
- Limit top-K results
- Emit diagnostics warnings

Output:
- `AggregationResult`

---

### MarkScoreSelector
Responsibilities:
- Select best score per mark
- Resolve ties deterministically
- Prepare data for suggestion creation

---

### SuggestionBuilder / SuggestionFactory
Responsibilities:
- Convert `MarkScore` → `MarkSuggestion`
- Ensure immutability and ordering stability

---

### SimilarityService
Responsibilities:
- Orchestrate full pipeline
- Call DB + preprocessing
- Collect metrics
- Enforce limits (K, distance)
- Return final suggestions

---

## Configuration

### ScoringPolicy
- `useRankWeighting`
- `perMarkDecay` (0–1)
- `fanOutStrategy` (SPLIT / FULL)

### SimilarityPolicy
- `maxK`
- `minScore`
- `parityEnabled`

### SanitizationPolicy
- `minSimilarity`
- `maxSimilarity`

### EmbeddingPolicy
- embedding dimension constraints

---

## Failure Modes & Safeguards

### 1. Invalid similarity values
→ filtered during sanitization

### 2. Empty weight sums
→ confidence = 0

### 3. Missing mark mappings
→ tracked via `missingMarkMappings`

### 4. Duplicate contributions
→ deduplicated via AggregationKey

### 5. Numeric instability
→ Kahan summation used for aggregation

---

## Test Strategy

Recommended tests:

### Unit tests
- grouping correctness
- decay behavior
- fan-out scaling
- deduplication logic
- confidence normalization

### Property-based tests
- ordering stability
- idempotence of aggregation
- bounded output [0,1]

### Integration tests
- DB → sanitizer → aggregation pipeline
- full suggestion generation

---

## Maintainability Notes

- Keep scoring logic inside `ScoreCalculator`
- Do not introduce side effects in grouper or builder
- Avoid adding business rules to service orchestrator
- Preserve deterministic ordering contracts

---

## Summary

This module implements a deterministic, policy-driven similarity aggregation engine with:

- stable ranking behavior
- configurable scoring model
- clear separation of concerns
- strong debugging visibility

It is designed to be safe under:
- noisy DB input
- duplicate evidence
- partial data
- evolving scoring policies