package pt.estga.processing.services.similarity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.processing.config.policies.SanitizationPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.SanitizationKey;
import pt.estga.processing.models.SanitizationResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Trust boundary: convert DB projections into domain-shaped
 * {@link pt.estga.processing.models.CandidateEvidence} and produce sanitized
 * outputs for aggregation.
 * <p>
 * Notes:
 * - Preserves DB ordering after removing null/invalid rows.
 * - Clamps out-of-range similarity values and counts anomalies.
 * - Produces occurrence-level keys for diagnostics; aggregation-time
 *   deduplication by (evidence, occurrence, mark) is performed by the
 *   aggregator.
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
        java.util.Set<SanitizationKey> candidateKeys = candidates.stream()
                .map(c -> SanitizationKey.of(c.evidenceId(), c.occurrenceId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new SanitizationResult(Collections.unmodifiableList(candidates), Collections.unmodifiableSet(idSet), Collections.unmodifiableSet(candidateKeys), hits.size(), invalid, outOfRange);
    }
}
