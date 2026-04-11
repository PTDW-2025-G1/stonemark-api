package pt.estga.processing.services.similarity.aggregation;

import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.enums.FanOutStrategy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.KahanState;
import pt.estga.processing.models.ScoringResult;
import pt.estga.processing.utils.KahanAccumulator;

import java.util.*;

public class MarkScoringStage {

    private final ScoringPolicy scoringPolicy;

    public MarkScoringStage(ScoringPolicy scoringPolicy) {
        this.scoringPolicy = scoringPolicy;
    }

    public ScoringResult score(Map<Long, List<CandidateEvidence>> dedupedByMark, Map<java.util.UUID, Integer> fanOutCounts) {
        Map<Long, KahanState> scoreStates = new TreeMap<>();
        Map<Long, KahanState> weightStates = new TreeMap<>();

        int perMarkContributions = 0;
        int perMarkDecayApplied = 0;
        int fanOutContributionCount = 0;

        double decay = Math.max(0.0, scoringPolicy.getPerMarkDecay());

        List<Long> markIds = new ArrayList<>(dedupedByMark.keySet());
        Collections.sort(markIds);
        for (Long markId : markIds) {
            List<CandidateEvidence> list = dedupedByMark.get(markId);
            if (list == null || list.isEmpty()) continue;

            // Do not mutate input lists: make a defensive copy and sort that.
            List<CandidateEvidence> sorted = new ArrayList<>(list);
            CandidateGrouper.sortGroupsEvidencesDeterministically(sorted);

            int used = 0;
            for (int i = 0; i < sorted.size(); i++) {
                CandidateEvidence ce = sorted.get(i);
                if (ce == null) continue;
                java.util.UUID evidenceId = ce.evidenceId();
                double similarity = ce.similarity();

                if (!Double.isFinite(similarity)) continue;
                double perMarkMultiplier = Math.pow(decay, used);
                if (used > 0) perMarkDecayApplied++;
                perMarkContributions++;

                double simClamped = Math.max(0.0, Math.min(1.0, similarity));
                double rankScore = computeRankScore(i);
                double signal = simClamped * (scoringPolicy.isUseRankWeighting() ? rankScore : 1.0);
                double contribution = signal * perMarkMultiplier;

                int fanOut = FanOutResolver.resolveFanOut(fanOutCounts, evidenceId);
                double scale = scoringPolicy.getFanOutStrategy() == FanOutStrategy.SPLIT ? 1.0 / (double) fanOut : 1.0;
                if (fanOut > 1) fanOutContributionCount++;

                KahanAccumulator.accumulate(scoreStates, markId, contribution * scale);
                KahanAccumulator.accumulate(weightStates, markId, perMarkMultiplier * scale);

                used++;
            }
        }

        // Produce corrected totals from per-key states
        Map<Long, Double> correctedScores = KahanAccumulator.toCorrectedMap(scoreStates);
        Map<Long, Double> correctedWeightSums = KahanAccumulator.toCorrectedMap(weightStates);

        int weightAnomalies = 0;
        final double MIN_ABS = 1e-12;
        final double REL_EPS = 1e-12;
        double maxWeight = 0.0;
        for (Double w : correctedWeightSums.values()) if (w != null && Double.isFinite(w)) maxWeight = Math.max(maxWeight, Math.abs(w));
        double threshold = Math.max(MIN_ABS, maxWeight * REL_EPS);
        for (Map.Entry<Long, Double> e : correctedWeightSums.entrySet()) {
            Double w = e.getValue();
            if (w == null || !Double.isFinite(w) || Math.abs(w) <= threshold) weightAnomalies++;
        }

        return new ScoringResult(correctedScores, correctedWeightSums, perMarkContributions, perMarkDecayApplied, fanOutContributionCount, weightAnomalies);
    }

    private static double computeRankScore(int i) {
        return 1.0 / (1.0 + (double) i);
    }
}
