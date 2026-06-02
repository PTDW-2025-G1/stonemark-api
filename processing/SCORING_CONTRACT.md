# Scoring Contract

This document defines the authoritative scoring contract for the processing aggregation
pipeline. Tests and implementation must adhere to these rules. Changes to the implementation
must update this contract first.

Contract summary

- Contribution
  - A contribution is a single CandidateEvidence (evidenceId, occurrenceId, similarity)
    expanded to a mark via the mark mapping (one evidence may map to multiple marks).
  - Similarity values are clamped to [0.0, 1.0] before any weighting or decay.

- Deduplication
  - Deduplication happens before scoring and before applying fan-out scaling.
  - Duplicate key = (evidenceId, occurrenceId, markId). When multiple entries share the same
    key, the one with strictly greater similarity wins. If similarity is equal, the first-seen
    entry (input order) wins. The duplicate counter increments for each removed entry.

- Fan-out
  - Fan-out (N) for an evidence is the number of distinct marks it contributes to after
    deduplication.
  - When FanOutStrategy.SPLIT is used, each contribution from an evidence is scaled by 1/N.
  - When FanOutStrategy.FULL is used, contributions are not scaled.

- Decay and Rank Weighting
  - Within a mark group, contributions are sorted deterministically and assigned a rank index
    starting at 0. A per-mark decay factor is applied: perMarkMultiplier = perMarkDecay ^ rankIndex.
  - Rank weighting multiplies similarity by (1 / (1 + rankIndex)) when enabled.

- Scoring Formula
  - For a contribution with similarity S, rank index i, per-mark decay D, fan-out scale F:
      simClamped = clamp(S, 0.0, 1.0)
      rankWeight = useRankWeighting ? 1.0 / (1.0 + i) : 1.0
      perMarkMultiplier = D ^ i
      contribution = simClamped * rankWeight * perMarkMultiplier * F
  - We accumulate contributions per mark using compensated summation (Kahan) to compute
    rawScores and weightSums (weightSums accumulates perMarkMultiplier * F).
  - Final confidence per mark = quantize( clamp( rawScore / weightSum, 0.0, 1.0 ) )
    where quantize rounds to 1e-6 units.

- Telemetry / Counters
  - duplicates: number of duplicate entries removed during deduplication.
  - fanOutContributionCount: number of processed contributions (after dedup) for which
    the evidence had fanOut > 1. This is counted per processed contribution (i.e., one per mark).
  - weightAnomalies: number of marks with weightSum <= 1e-12.

Notes
- Ordering is deterministic but not semantically meaningful beyond tie-breaking. Tests should assert
  business invariants (top-K ordering, dedup correctness, fan-out correctness) and avoid relying on
  implementation-only ordering unless explicitly documented here.
