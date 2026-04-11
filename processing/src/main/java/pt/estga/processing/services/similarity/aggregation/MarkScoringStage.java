package pt.estga.processing.services.similarity.aggregation;

import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.enums.FanOutStrategy;
import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

public class MarkScoringStage {

    private final ScoringPolicy scoringPolicy;

    public MarkScoringStage(ScoringPolicy scoringPolicy) {
        this.scoringPolicy = scoringPolicy;
    }

    public static record ScoringResult(Map<Long, Double> scores, Map<Long, Double> weightSums, int perMarkContributions, int perMarkDecayApplied, int fanOutContributionCount, int weightAnomalies) {}

    public ScoringResult score(Map<Long, List<CandidateEvidence>> dedupedByMark, Map<java.util.UUID, Integer> fanOutCounts) {
        Map<Long, Double> scores = new TreeMap<>();
        Map<Long, Double> weightSums = new TreeMap<>();
        Map<Long, Double> scoreComps = new TreeMap<>();
        Map<Long, Double> weightComps = new TreeMap<>();

        int perMarkContributions = 0;
        int perMarkDecayApplied = 0;
        int fanOutContributionCount = 0;

        double decay = Math.max(0.0, scoringPolicy.getPerMarkDecay());

        List<Long> markIds = new ArrayList<>(dedupedByMark.keySet());
        Collections.sort(markIds);
        for (Long markId : markIds) {
            List<CandidateEvidence> list = dedupedByMark.get(markId);
            if (list == null || list.isEmpty()) continue;

            // Sort deterministically within the group
            CandidateGrouper.sortGroupsEvidencesDeterministically(list);

            int used = 0;
            for (int i = 0; i < list.size(); i++) {
                CandidateEvidence ce = list.get(i);
                if (ce == null) continue;
                java.util.UUID evidenceId = ce.evidenceId();
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

                KahanAccumulator.kahanScoresSum(scores, scoreComps, markId, contribution * scale);
                KahanAccumulator.kahanScoresSum(weightSums, weightComps, markId, perMarkMultiplier * scale);

                used++;
            }
        }

        int weightAnomalies = 0;
        final double MIN_WEIGHT = 1e-12;
        for (Map.Entry<Long, Double> e : weightSums.entrySet()) {
            Double w = e.getValue();
            if (w == null || w <= MIN_WEIGHT) weightAnomalies++;
        }

        return new ScoringResult(scores, weightSums, perMarkContributions, perMarkDecayApplied, fanOutContributionCount, weightAnomalies);
    }
}
