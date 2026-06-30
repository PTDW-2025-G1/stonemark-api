# Scoring Contract

The `SimilarityService` produces mark suggestions from a Vision embedding using pgvector cosine similarity.

## Pipeline (simplified)

1. Convert embedding to pgvector literal string
2. Query `MarkEvidenceQueryService.findTopKSimilar(vector, k, maxDistance)` — pgvector cosine similarity
3. Filter results: reject null, NaN, Infinity values
4. Fetch mark associations: `findMarksByEvidenceIds(evidenceIds)`
5. Group by `markId`, sum similarity contributions
6. Sort by summed confidence descending, then by `markId` ascending (tiebreaker)
7. Limit to top `k` results
8. Create `MarkSuggestion` entities with confidence quantized to 6 decimal places

## Scoring Formula

```
For each evidence hit with similarity S:
  confidence[markId] += S

confidence is summed directly — no decay, fan-out, or rank weighting.
```

## Invariants

- Output is sorted: confidence descending, then markId ascending
- Confidence is clamped to [0.0, 1.0] and quantized to 1e-6
- Empty or missing embedding → empty result list
- No valid hits after filtering → empty result list
- `maxDistance = 1.0 - minScore` (configured via `processing.similarity.minScore`)

## Determinism

- `LinkedHashMap` for mark group iteration (insertion order from evidence ID list)
- `Map.Entry.comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())` for sorting
- Confidence quantization to 6 decimal places ensures stable comparisons
