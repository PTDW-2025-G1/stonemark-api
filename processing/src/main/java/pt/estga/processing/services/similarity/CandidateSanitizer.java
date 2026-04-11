package pt.estga.processing.services.similarity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.processing.config.policies.SanitizationPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.SanitizationResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Trust boundary: converts DB projections into domain-shaped CandidateEvidence
 * instances and returns counts and sets useful for orchestration.
 *
 * Contract guarantees:
 * - The returned {@code candidates} list preserves the input (DB) ordering after
 *   removing null rows and invalid similarity values. Candidates are not deduplicated
 *   in this list; duplicates may appear if the DB returned duplicated projections.
 * - The returned {@code idSet} is a LinkedHashSet preserving the first-seen order of
 *   evidence ids and contains one entry per distinct evidence id.
 * - The returned {@code candidateKeys} is a LinkedHashSet of deduplicated
 *   (evidenceId, occurrenceId) pairs in the order they were first seen; it is useful
 *   for detecting duplicate occurrence-level contributions without mutating the
 *   primary candidates list.
 * - Similarity values outside configured bounds are clamped (not dropped) and
 *   counted in {@code outOfRangeCount}.
 * - The sanitizer does NOT enforce aggregation-time deduplication by mark.
 *   It is the aggregator's responsibility to enforce uniqueness of
 *   (evidenceId, occurrenceId, markId) contributions; sanitizer only provides
 *   occurrence-level keys for higher-level orchestration and diagnostics.
 */
@Service
@RequiredArgsConstructor
public class CandidateSanitizer {

    private final SanitizationPolicy policy;

    public SanitizationResult sanitize(List<MarkEvidenceDistanceProjection> hits) {
        if (hits == null || hits.isEmpty()) {
            return new SanitizationResult(List.of(), Collections.emptySet(), Collections.emptySet(), 0, 0, 0);
        }

        List<MarkEvidenceDistanceProjection> nonNull = hits.stream().filter(Objects::nonNull).toList();
        List<CandidateEvidence> candidates = new ArrayList<>();
        int invalid = 0;
        int outOfRange = 0;
        for (MarkEvidenceDistanceProjection p : nonNull) {
            Double raw = p.getSimilarity();
            if (raw == null || raw.isNaN()) { invalid++; continue; }
            double sim = raw;
            if (!Double.isFinite(sim)) { invalid++; continue; }
            if (sim > policy.getMaxSimilarity() || sim < policy.getMinSimilarity()) { outOfRange++; sim = Math.max(policy.getMinSimilarity(), Math.min(policy.getMaxSimilarity(), sim)); }
            UUID eid = p.id();
            Long occ;
            try { occ = p.getOccurrenceId(); } catch (Throwable t) { occ = null; }
            candidates.add(new CandidateEvidence(eid, occ, sim));
        }

        Set<UUID> idSet = candidates.stream().map(CandidateEvidence::evidenceId).collect(Collectors.toCollection(LinkedHashSet::new));
        java.util.Set<pt.estga.processing.models.CandidateKey> candidateKeys = candidates.stream()
                .map(c -> pt.estga.processing.models.CandidateKey.of(c.evidenceId(), c.occurrenceId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new SanitizationResult(Collections.unmodifiableList(candidates), Collections.unmodifiableSet(idSet), Collections.unmodifiableSet(candidateKeys), hits.size(), invalid, outOfRange);
    }
}
