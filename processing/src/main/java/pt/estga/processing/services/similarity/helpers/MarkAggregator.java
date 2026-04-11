package pt.estga.processing.services.similarity.helpers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.config.ProcessingProperties;
import pt.estga.processing.models.AggregationResult;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.EvidenceKey;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core business logic: aggregate CandidateEvidence into per-mark scores and weight sums.
 * This class does not perform DB access or emit metrics.
 */
@Service
@RequiredArgsConstructor
public class MarkAggregator {

    private final ProcessingProperties properties;

    public AggregationResult aggregate(List<CandidateEvidence> candidates, Map<UUID, Mark> markByEvidenceId) {
        Map<Long, Double> scores = new TreeMap<>();
        Map<Long, Double> weightSums = new TreeMap<>();
        Map<Long, Double> scoreComps = new TreeMap<>();
        Map<Long, Double> weightComps = new TreeMap<>();

        // Build marksById map
        Map<Long, Mark> marksById = markByEvidenceId.values().stream().filter(Objects::nonNull)
                .filter(m -> m.getId() != null)
                .collect(Collectors.toMap(Mark::getId, m -> m, (a,b) -> a, TreeMap::new));

        // Group candidates by mark id
        Map<Long, List<CandidateEvidence>> contributionsByMark = new HashMap<>();
        for (CandidateEvidence c : candidates) {
            Mark mark = markByEvidenceId.get(c.evidenceId());
            if (mark == null || mark.getId() == null) continue;
            Long markId = mark.getId();
            // Avoid unused-lambda warnings by using explicit put/get
            if (!contributionsByMark.containsKey(markId)) {
                contributionsByMark.put(markId, new ArrayList<>());
            }
            contributionsByMark.get(markId).add(c);
        }

        Set<EvidenceKey> seenPairs = new HashSet<>();
        int duplicates = 0;
        int perMarkContributions = 0;
        int perMarkDecayApplied = 0;

        List<Long> markIds = new ArrayList<>(contributionsByMark.keySet());
        Collections.sort(markIds);
        for (Long markId : markIds) {
            List<CandidateEvidence> list = contributionsByMark.get(markId);
            if (list == null || list.isEmpty()) continue;
            list.sort((a,b) -> {
                int cmp = Double.compare(b.similarity(), a.similarity());
                if (cmp != 0) return cmp;
                Long oa = a.occurrenceId();
                Long ob = b.occurrenceId();
                if (oa == null && ob != null) return -1;
                if (oa != null && ob == null) return 1;
                if (oa != null) {
                    int c = oa.compareTo(ob);
                    if (c != 0) return c;
                }
                return a.evidenceId().toString().compareTo(b.evidenceId().toString());
            });

            int used = 0;
            double decay = Math.max(0.0, properties.getSimilarity().getPerMarkDecay());
            for (int i = 0; i < list.size(); i++) {
                CandidateEvidence ce = list.get(i);
                UUID evidenceId = ce.evidenceId();
                double similarity = ce.similarity();

                EvidenceKey key = new EvidenceKey(evidenceId, markId, ce.occurrenceId());
                if (!seenPairs.add(key)) { duplicates++; continue; }

                if (Double.isNaN(similarity)) continue;
                double perMarkMultiplier = Math.pow(decay, used);
                if (used > 0) perMarkDecayApplied++;
                perMarkContributions++;

                double simClamped = Math.max(0.0, Math.min(1.0, similarity));
                double rankScore = 1.0 / (1.0 + (double)i);
                double signal = simClamped * (properties.getSimilarity().isUseRankWeighting() ? rankScore : 1.0);
                double contribution = signal * perMarkMultiplier;

                // Kahan sum for scores
                double s = scores.getOrDefault(markId, 0.0);
                double sc = scoreComps.getOrDefault(markId, 0.0);
                double y = contribution - sc;
                double t = s + y;
                sc = (t - s) - y;
                s = t;
                scores.put(markId, s);
                scoreComps.put(markId, sc);

                // Kahan sum for weights
                double w = weightSums.getOrDefault(markId, 0.0);
                double wc = weightComps.getOrDefault(markId, 0.0);
                double wy = perMarkMultiplier - wc;
                double wt = w + wy;
                wc = (wt - w) - wy;
                w = wt;
                weightSums.put(markId, w);
                weightComps.put(markId, wc);

                used++;
            }
        }

        return new AggregationResult(scores, weightSums, marksById, duplicates, perMarkContributions, perMarkDecayApplied);
    }
}
