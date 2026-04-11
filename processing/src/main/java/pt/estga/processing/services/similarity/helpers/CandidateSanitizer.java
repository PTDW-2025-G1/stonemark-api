package pt.estga.processing.services.similarity.helpers;

import org.springframework.stereotype.Service;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.SanitizationResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Trust boundary: converts DB projections into domain-shaped CandidateEvidence
 * instances and returns counts useful for orchestration.
 */
@Service
public class CandidateSanitizer {

    public SanitizationResult sanitize(List<MarkEvidenceDistanceProjection> hits) {
        if (hits == null || hits.isEmpty()) return new SanitizationResult(List.of(), Collections.emptySet(), 0, 0, 0);

        List<MarkEvidenceDistanceProjection> nonNull = hits.stream().filter(Objects::nonNull).toList();
        List<CandidateEvidence> candidates = new ArrayList<>();
        int invalid = 0;
        int outOfRange = 0;
        for (MarkEvidenceDistanceProjection p : nonNull) {
            Double raw = p.getSimilarity();
            if (raw == null || raw.isNaN()) { invalid++; continue; }
            double sim = raw;
            if (!Double.isFinite(sim)) { invalid++; continue; }
            if (sim > 1.0 || sim < 0.0) { outOfRange++; sim = Math.max(0.0, Math.min(1.0, sim)); }
            UUID eid = p.getId();
            Long occ;
            try { occ = p.getOccurrenceId(); } catch (Throwable t) { occ = null; }
            candidates.add(new CandidateEvidence(eid, occ, sim));
        }

        Set<UUID> idSet = candidates.stream().map(CandidateEvidence::evidenceId).collect(Collectors.toCollection(LinkedHashSet::new));
        return new SanitizationResult(Collections.unmodifiableList(candidates), Collections.unmodifiableSet(idSet), hits.size(), invalid, outOfRange);
    }
}
