package pt.estga.processing.services.similarity.aggregation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.enums.FanOutStrategy;
import pt.estga.processing.models.AggregationState;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.AggregationKey;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ScoreCalculator {

    private final ScoringPolicy scoringPolicy;

    /**
     * Computes per-mark scores and weight sums from grouped candidate contributions.
     *
     * <p>Key behavior:
     * <ul>
     *   <li>Deduplicates contributions by (evidenceId, occurrenceId, markId), keeping the
     *       highest-similarity entry per group.</li>
     *   <li>Decay slots are consumed only by valid, non-duplicate contributions.</li>
     *   <li>Fan-out is computed from grouped contributions (evidence → distinct marks),
     *       so upstream filtering affects scaling.</li>
     * </ul>
     */
    public AggregationState compute(Map<Long, List<CandidateEvidence>> contributionsByMark) {
        Map<Long, Double> scores = new TreeMap<>();
        Map<Long, Double> weightSums = new TreeMap<>();
        Map<Long, Double> scoreComps = new TreeMap<>();
        Map<Long, Double> weightComps = new TreeMap<>();

        int duplicates = 0;
        int perMarkContributions = 0;
        int perMarkDecayApplied = 0;
        int fanOutContributionCount = 0;

        double decay = Math.max(0.0, scoringPolicy.getPerMarkDecay());

        // Compute evidence -> distinct mark set for fan-out scaling
        Map<UUID, Set<Long>> evidenceToMarks = new HashMap<>();
        for (Map.Entry<Long, List<CandidateEvidence>> e : contributionsByMark.entrySet()) {
            Long markId = e.getKey();
            for (CandidateEvidence ce : e.getValue()) {
                evidenceToMarks.computeIfAbsent(ce.evidenceId(), _ -> new HashSet<>()).add(markId);
            }
        }
        Map<UUID, Integer> fanOutCounts = new HashMap<>();
        for (Map.Entry<UUID, Set<Long>> e : evidenceToMarks.entrySet()) {
            fanOutCounts.put(e.getKey(), e.getValue().size());
        }

        List<Long> markIds = new ArrayList<>(contributionsByMark.keySet());
        Collections.sort(markIds);
        for (Long markId : markIds) {
            List<CandidateEvidence> list = contributionsByMark.get(markId);
            if (list == null || list.isEmpty()) continue;

            // Deduplicate by AggregationKey keeping the highest-similarity entry per key.
            Map<AggregationKey, CandidateEvidence> bestPerKey = new LinkedHashMap<>();
            for (CandidateEvidence ce : list) {
                if (ce == null) continue; // tolerate null entries in lists
                AggregationKey key = AggregationKey.of(ce.evidenceId(), ce.occurrenceId(), markId);
                CandidateEvidence prev = bestPerKey.get(key);
                if (prev == null) {
                    bestPerKey.put(key, ce);
                } else {
                    // If a later entry has higher similarity, replace and count as duplicate.
                    if (Double.compare(ce.similarity(), prev.similarity()) > 0) {
                        bestPerKey.put(key, ce);
                    }
                    duplicates++;
                }
            }

            // Now process the deduplicated contributions in deterministic order.
            List<CandidateEvidence> deduped = new ArrayList<>(bestPerKey.values());
            // Sort to maintain the same within-group ordering expected by the algorithm
            CandidateGrouper.sortGroupsEvidencesDeterministically(deduped);

            int used = 0;
            for (int i = 0; i < deduped.size(); i++) {
                CandidateEvidence ce = deduped.get(i);
                UUID evidenceId = ce.evidenceId();
                double similarity = ce.similarity();

                if (!Double.isFinite(similarity)) continue;
                double perMarkMultiplier = Math.pow(decay, used);
                if (used > 0) perMarkDecayApplied++;
                perMarkContributions++;

                double simClamped = Math.max(0.0, Math.min(1.0, similarity));
                double rankScore = 1.0 / (1.0 + (double)i);
                double signal = simClamped * (scoringPolicy.isUseRankWeighting() ? rankScore : 1.0);
                double contribution = signal * perMarkMultiplier;

                int fanOut = fanOutCounts.getOrDefault(evidenceId, 1);
                double scale = scoringPolicy.getFanOutStrategy() == FanOutStrategy.SPLIT ? 1.0 / Math.max(1, fanOut) : 1.0;
                if (fanOut > 1) fanOutContributionCount++;

                kahanScoresSum(scores, scoreComps, markId, contribution * scale);
                kahanScoresSum(weightSums, weightComps, markId, perMarkMultiplier * scale);

                used++;
            }
        }

        int weightAnomalies = 0;
        final double MIN_WEIGHT = 1e-12;
        for (Map.Entry<Long, Double> e : weightSums.entrySet()) {
            Double w = e.getValue();
            if (w == null || w <= MIN_WEIGHT) weightAnomalies++;
        }

        return new AggregationState(scores, weightSums, duplicates, perMarkContributions, perMarkDecayApplied, fanOutContributionCount, weightAnomalies);
    }

    private void kahanScoresSum(Map<Long, Double> weightSums, Map<Long, Double> weightComps, Long markId, double perMarkMultiplier) {
        double w = weightSums.getOrDefault(markId, 0.0);
        double wc = weightComps.getOrDefault(markId, 0.0);
        double wy = perMarkMultiplier - wc;
        double wt = w + wy;
        wc = (wt - w) - wy;
        w = wt;
        weightSums.put(markId, w);
        weightComps.put(markId, wc);
    }
}
