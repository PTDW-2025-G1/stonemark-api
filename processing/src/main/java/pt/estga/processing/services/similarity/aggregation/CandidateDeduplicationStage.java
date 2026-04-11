package pt.estga.processing.services.similarity.aggregation;

import pt.estga.processing.models.AggregationKey;
import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

public class CandidateDeduplicationStage {

    public static record DeduplicationResult(Map<Long, List<CandidateEvidence>> deduped, int duplicates) {}

    public DeduplicationResult deduplicate(Map<Long, List<CandidateEvidence>> normalizedByMark) {
        Map<Long, List<CandidateEvidence>> result = new TreeMap<>();
        if (normalizedByMark == null || normalizedByMark.isEmpty()) return new DeduplicationResult(result, 0);

        int duplicates = 0;
        for (Map.Entry<Long, List<CandidateEvidence>> e : normalizedByMark.entrySet()) {
            Long markId = e.getKey();
            List<CandidateEvidence> list = e.getValue();
            if (list == null || list.isEmpty()) continue;

            Map<AggregationKey, CandidateEvidence> bestPerKey = new LinkedHashMap<>();
            for (CandidateEvidence ce : list) {
                if (ce == null) continue;
                AggregationKey key = AggregationKey.of(ce.evidenceId(), ce.occurrenceId(), markId);
                CandidateEvidence prev = bestPerKey.get(key);
                if (prev == null) {
                    bestPerKey.put(key, ce);
                } else {
                    if (Double.compare(ce.similarity(), prev.similarity()) > 0) {
                        bestPerKey.put(key, ce);
                    }
                    duplicates++;
                }
            }

            List<CandidateEvidence> deduped = new ArrayList<>(bestPerKey.values());
            result.put(markId, deduped);
        }

        return new DeduplicationResult(result, duplicates);
    }
}
