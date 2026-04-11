package pt.estga.processing.services.similarity.aggregation;

import pt.estga.processing.models.CandidateEvidence;

import java.util.*;

/**
 * Normalization stage: cleans and clamps candidate evidences.
 */
public class CandidateNormalizationStage {

    public Map<Long, List<CandidateEvidence>> normalize(Map<Long, List<CandidateEvidence>> contributionsByMark) {
        Map<Long, List<CandidateEvidence>> normalized = new TreeMap<>();
        if (contributionsByMark == null || contributionsByMark.isEmpty()) return normalized;

        for (Map.Entry<Long, List<CandidateEvidence>> e : contributionsByMark.entrySet()) {
            Long markId = e.getKey();
            List<CandidateEvidence> list = e.getValue();
            if (list == null || list.isEmpty()) continue;
            List<CandidateEvidence> out = new ArrayList<>();
            for (CandidateEvidence ce : list) {
                if (ce == null) continue;
                double raw = ce.similarity();
                if (Double.isNaN(raw) || !Double.isFinite(raw)) continue;
                double sim = Math.max(0.0, Math.min(1.0, raw));
                out.add(new CandidateEvidence(ce.evidenceId(), ce.occurrenceId(), sim));
            }
            if (!out.isEmpty()) normalized.put(markId, out);
        }

        return normalized;
    }
}
