package pt.estga.processing.services.similarity.aggregation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.AggregationState;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.CandidateKey;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ScoreCalculator {

    private final ScoringPolicy scoringPolicy;

    public AggregationState compute(Map<Long, List<CandidateEvidence>> contributionsByMark) {
        Map<Long, Double> scores = new TreeMap<>();
        Map<Long, Double> weightSums = new TreeMap<>();
        Map<Long, Double> scoreComps = new TreeMap<>();
        Map<Long, Double> weightComps = new TreeMap<>();

        Set<CandidateKey> seenPairs = new HashSet<>();
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

                CandidateKey key = CandidateKey.of(evidenceId, ce.occurrenceId(), markId);
                if (!seenPairs.add(key)) { duplicates++; continue; }

                if (Double.isNaN(similarity)) continue;
                double perMarkMultiplier = Math.pow(decay, used);
                if (used > 0) perMarkDecayApplied++;
                perMarkContributions++;

                double simClamped = Math.max(0.0, Math.min(1.0, similarity));
                double rankScore = 1.0 / (1.0 + (double)i);
                double signal = simClamped * (scoringPolicy.isUseRankWeighting() ? rankScore : 1.0);
                double contribution = signal * perMarkMultiplier;

                kahanScoresSum(scores, scoreComps, markId, contribution);
                kahanScoresSum(weightSums, weightComps, markId, perMarkMultiplier);

                used++;
            }
        }

        // Compute confidences (truth of confidence = score / weight) here to centralize
        // the scoring definition in a single place and avoid divergence. Guard against
        // extremely small or missing weights to prevent division anomalies.
        final double MIN_WEIGHT = 1e-12;
        Map<Long, Double> confidences = new TreeMap<>();
        int weightAnomalies = 0;
        for (Map.Entry<Long, Double> e : scores.entrySet()) {
            Long markId = e.getKey();
            double totalScore = e.getValue();
            Double weight = weightSums.get(markId);
            if (weight == null || weight <= MIN_WEIGHT) {
                // Record anomaly: no safe normalization possible; set confidence to 0.0
                weightAnomalies++;
                confidences.put(markId, 0.0);
                continue;
            }
            double conf = totalScore / weight;
            // Clamp confidence into [0,1] to avoid surprises from numeric instability
            conf = Math.max(0.0, Math.min(1.0, conf));
            confidences.put(markId, conf);
        }

        return new AggregationState(scores, weightSums, confidences, duplicates, perMarkContributions, perMarkDecayApplied, weightAnomalies);
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
