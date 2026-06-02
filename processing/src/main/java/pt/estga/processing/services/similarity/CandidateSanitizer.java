package pt.estga.processing.services.similarity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.processing.config.ProcessingProperties;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.SanitizationKey;
import pt.estga.processing.models.SanitizationResult;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateSanitizer {

    private final ProcessingProperties properties;

    public SanitizationResult sanitize(List<MarkEvidenceDistanceDto> hits) {
        if (hits == null || hits.isEmpty()) {
            return new SanitizationResult(List.of(), Collections.emptySet(), Collections.emptySet(), 0, 0, 0);
        }

        List<MarkEvidenceDistanceDto> nonNull = hits.stream().filter(Objects::nonNull).toList();
        List<CandidateEvidence> candidates = new ArrayList<>();
        int invalid = 0;
        int outOfRange = 0;
        for (MarkEvidenceDistanceDto p : nonNull) {
            Double raw = p.similarity();
            if (raw == null || raw.isNaN()) { invalid++; continue; }
            double sim = raw;
            if (!Double.isFinite(sim)) { invalid++; continue; }
            if (sim > properties.similarity().maxSimilarity() || sim < properties.similarity().minSimilarity()) { outOfRange++; sim = Math.max(properties.similarity().minSimilarity(), Math.min(properties.similarity().maxSimilarity(), sim)); }
            UUID eid = p.id();
            Long occ = p.occurrenceId();
            candidates.add(new CandidateEvidence(eid, occ, sim));
        }

        Set<UUID> idSet = candidates.stream().map(CandidateEvidence::evidenceId).collect(Collectors.toCollection(LinkedHashSet::new));
        java.util.Set<SanitizationKey> candidateKeys = candidates.stream()
                .map(c -> SanitizationKey.of(c.evidenceId(), c.occurrenceId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new SanitizationResult(Collections.unmodifiableList(candidates), Collections.unmodifiableSet(idSet), Collections.unmodifiableSet(candidateKeys), hits.size(), invalid, outOfRange);
    }
}
