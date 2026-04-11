package pt.estga.processing.services.similarity.helpers.aggregation;

import org.springframework.stereotype.Component;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.EvidenceKey;

import java.util.*;

@Component
public class ScoreCalculator {

    private final ScoringPolicy scoringPolicy;

    public ScoreCalculator(ScoringPolicy scoringPolicy) {
        this.scoringPolicy = scoringPolicy;
    }

    public AggregationState compute(Map<Long, List<CandidateEvidence>> contributionsByMark) {
        Map<Long, Double> scores = new TreeMap<>();
        Map<Long, Double> weightSums = new TreeMap<>();
        Map<Long, Double> scoreComps = new TreeMap<>();
        Map<Long, Double> weightComps = new TreeMap<>();

        Set<EvidenceKey> seenPairs = new HashSet<>();
        int duplicates = 0;
        int perMarkContributions = 0;
        int perMarkDecayApplied = 0;

        double decay = Math.max(0.0, scoringPolicy.getPerMarkDecay());

        List<Long> markIds = new ArrayList<>(contributionsByMark.keySet());
        Collections.sort(markIds);
        for (Long markId : markIds) {
            List<CandidateEvidence> list = contributionsByMark.get(markId);
            if (list == null || list.isEmpty()) continue;

            int used = 0;
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
                double signal = simClamped * (scoringPolicy.isUseRankWeighting() ? rankScore : 1.0);
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

        return new AggregationState(scores, weightSums, duplicates, perMarkContributions, perMarkDecayApplied);
    }

    public static class AggregationState {
        public final Map<Long, Double> scores;
        public final Map<Long, Double> weightSums;
        public final int duplicates;
        public final int perMarkContributions;
        public final int perMarkDecayApplied;

        public AggregationState(Map<Long, Double> scores, Map<Long, Double> weightSums, int duplicates, int perMarkContributions, int perMarkDecayApplied) {
            this.scores = scores;
            this.weightSums = weightSums;
            this.duplicates = duplicates;
            this.perMarkContributions = perMarkContributions;
            this.perMarkDecayApplied = perMarkDecayApplied;
        }
    }
}
