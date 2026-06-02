package pt.estga.processing.services.similarity.aggregation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.processing.config.ProcessingProperties;
import pt.estga.processing.enums.FanOutStrategy;
import pt.estga.processing.models.AggregationKey;
import pt.estga.processing.models.AggregationResult;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.models.MarkScore;

import java.util.*;

@Service
@Slf4j
public class MarkAggregator {

    private final ProcessingProperties props;

    public MarkAggregator(ProcessingProperties props) {
        this.props = props;
    }

    public AggregationResult aggregate(List<CandidateEvidence> candidates,
                                        Map<UUID, List<Long>> markIdsByEvidenceId,
                                        int k,
                                        int missingMarkMappings) {
        var contributionsByMark = groupByMark(candidates, markIdsByEvidenceId);
        if (contributionsByMark.isEmpty()) {
            return new AggregationResult(List.of(), 0, 0, 0, 0,
                    Map.of(), Map.of(), missingMarkMappings, 0);
        }

        Set<Long> validMarkIds = collectValidMarkIds(markIdsByEvidenceId);

        var normalized = normalize(contributionsByMark);
        var fanOutCounts = computeFanOut(contributionsByMark);
        var dedupResult = deduplicate(normalized);
        var scoreResult = score(dedupResult.deduped(), fanOutCounts);

        return buildResult(scoreResult, dedupResult.duplicates(),
                validMarkIds, k, missingMarkMappings);
    }

    // ── grouping ────────────────────────────────────────────────────────

    private static Set<Long> collectValidMarkIds(Map<UUID, List<Long>> markIdsByEvidenceId) {
        Set<Long> valid = new TreeSet<>();
        if (markIdsByEvidenceId == null) return valid;
        for (List<Long> ids : markIdsByEvidenceId.values()) {
            if (ids == null) continue;
            for (Long id : ids) {
                if (id != null) valid.add(id);
            }
        }
        return valid;
    }

    private static Map<Long, List<CandidateEvidence>> groupByMark(
            List<CandidateEvidence> candidates,
            Map<UUID, List<Long>> markIdsByEvidenceId) {
        Map<Long, List<CandidateEvidence>> out = new TreeMap<>();
        if (candidates == null || candidates.isEmpty()) return out;
        if (markIdsByEvidenceId == null) return out;
        for (CandidateEvidence c : candidates) {
            List<Long> markIds = markIdsByEvidenceId.get(c.evidenceId());
            if (markIds == null || markIds.isEmpty()) continue;
            for (Long markId : markIds) {
                if (markId == null) continue;
                out.computeIfAbsent(markId, _ -> new ArrayList<>()).add(c);
            }
        }
        return out;
    }

    // ── normalization ────────────────────────────────────────────────────

    private static Map<Long, List<CandidateEvidence>> normalize(
            Map<Long, List<CandidateEvidence>> contributionsByMark) {
        Map<Long, List<CandidateEvidence>> out = new TreeMap<>();
        for (var e : contributionsByMark.entrySet()) {
            List<CandidateEvidence> in = e.getValue();
            if (in == null || in.isEmpty()) continue;
            List<CandidateEvidence> clean = new ArrayList<>();
            for (CandidateEvidence ce : in) {
                if (ce == null) continue;
                double raw = ce.similarity();
                if (Double.isNaN(raw) || !Double.isFinite(raw)) continue;
                clean.add(new CandidateEvidence(ce.evidenceId(), ce.occurrenceId(),
                        Math.max(0.0, Math.min(1.0, raw))));
            }
            if (!clean.isEmpty()) out.put(e.getKey(), clean);
        }
        return out;
    }

    // ── fan-out ─────────────────────────────────────────────────────────

    private static Map<UUID, Integer> computeFanOut(
            Map<Long, List<CandidateEvidence>> dedupedByMark) {
        Map<UUID, Set<Long>> evidenceToMarks = new HashMap<>();
        for (var e : dedupedByMark.entrySet()) {
            List<CandidateEvidence> list = e.getValue();
            if (list == null || list.isEmpty()) continue;
            for (CandidateEvidence ce : list) {
                if (ce == null || ce.evidenceId() == null) continue;
                evidenceToMarks.computeIfAbsent(ce.evidenceId(), _ -> new HashSet<>()).add(e.getKey());
            }
        }
        Map<UUID, Integer> fanOutCounts = new HashMap<>();
        for (var e : evidenceToMarks.entrySet()) {
            fanOutCounts.put(e.getKey(), e.getValue().size());
        }
        return fanOutCounts;
    }

    static int clampFanOut(int value, UUID evidenceId) {
        if (value <= 0) {
            log.warn("Invalid fanOut ({}) for evidence {} — clamping to 1", value, evidenceId);
            return 1;
        }
        return value;
    }

    static int resolveFanOut(Map<UUID, Integer> fanOutCounts, UUID evidenceId) {
        if (fanOutCounts == null) return 1;
        Integer v = fanOutCounts.get(evidenceId);
        return v == null ? 1 : clampFanOut(v, evidenceId);
    }

    // ── deduplication ────────────────────────────────────────────────────

    private record DedupResult(Map<Long, List<CandidateEvidence>> deduped, int duplicates) {}

    private static DedupResult deduplicate(Map<Long, List<CandidateEvidence>> normalizedByMark) {
        Map<Long, List<CandidateEvidence>> result = new TreeMap<>();
        int duplicates = 0;
        for (var e : normalizedByMark.entrySet()) {
            List<CandidateEvidence> list = e.getValue();
            if (list == null || list.isEmpty()) continue;
            Map<AggregationKey, CandidateEvidence> bestPerKey = new LinkedHashMap<>();
            for (CandidateEvidence ce : list) {
                if (ce == null) continue;
                AggregationKey key = AggregationKey.of(ce.evidenceId(), ce.occurrenceId(), e.getKey());
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
            result.put(e.getKey(), new ArrayList<>(bestPerKey.values()));
        }
        return new DedupResult(result, duplicates);
    }

    // ── scoring ──────────────────────────────────────────────────────────

    private record ScoreResult(Map<Long, Double> scores,
                                Map<Long, Double> weightSums,
                                int perMarkContributions,
                                int perMarkDecayApplied,
                                int fanOutContributionCount,
                                int weightAnomalies) {}

    private ScoreResult score(Map<Long, List<CandidateEvidence>> dedupedByMark,
                               Map<UUID, Integer> fanOutCounts) {
        Map<Long, Double> scores = new TreeMap<>();
        Map<Long, Double> weightSums = new TreeMap<>();
        int perMarkContributions = 0;
        int perMarkDecayApplied = 0;
        int fanOutContributionCount = 0;
        double decay = Math.max(0.0, props.similarity().perMarkDecay());
        FanOutStrategy fanOutStrategy = props.similarity().fanOutStrategy();
        boolean useRankWeighting = props.similarity().useRankWeighting();

        List<Long> markIds = new ArrayList<>(dedupedByMark.keySet());
        Collections.sort(markIds);
        for (Long markId : markIds) {
            List<CandidateEvidence> list = dedupedByMark.get(markId);
            if (list == null || list.isEmpty()) continue;

            List<CandidateEvidence> sorted = new ArrayList<>(list);
            sortGroupsEvidencesDeterministically(sorted);

            int used = 0;
            for (int i = 0; i < sorted.size(); i++) {
                CandidateEvidence ce = sorted.get(i);
                if (ce == null) continue;
                double sim = ce.similarity();
                if (!Double.isFinite(sim)) continue;

                double perMarkMultiplier = Math.pow(decay, used);
                if (used > 0) perMarkDecayApplied++;
                perMarkContributions++;

                sim = Math.max(0.0, Math.min(1.0, sim));
                double rankScore = 1.0 / (1.0 + (double) i);
                double signal = sim * (useRankWeighting ? rankScore : 1.0);
                double contribution = signal * perMarkMultiplier;

                int fanOut = resolveFanOut(fanOutCounts, ce.evidenceId());
                double scale = fanOutStrategy == FanOutStrategy.SPLIT ? 1.0 / (double) fanOut : 1.0;
                if (fanOut > 1) fanOutContributionCount++;

                scores.merge(markId, contribution * scale, Double::sum);
                weightSums.merge(markId, perMarkMultiplier * scale, Double::sum);
                used++;
            }
        }

        int weightAnomalies = 0;
        final double MIN_ABS = 1e-12;
        final double REL_EPS = 1e-12;
        double maxWeight = 0.0;
        for (Double w : weightSums.values()) {
            if (w != null && Double.isFinite(w)) maxWeight = Math.max(maxWeight, Math.abs(w));
        }
        double threshold = Math.max(MIN_ABS, maxWeight * REL_EPS);
        for (Double w : weightSums.values()) {
            if (w == null || !Double.isFinite(w) || Math.abs(w) <= threshold) weightAnomalies++;
        }

        return new ScoreResult(scores, weightSums, perMarkContributions,
                perMarkDecayApplied, fanOutContributionCount, weightAnomalies);
    }

    // ── result building ──────────────────────────────────────────────────

    private static AggregationResult buildResult(ScoreResult sr, int duplicates,
                                                  Set<Long> validMarkIds, int k,
                                                  int missingMarkMappings) {
        List<MarkScore> topScores = new ArrayList<>();
        final double MIN_WEIGHT = 1e-12;
        for (var e : sr.scores().entrySet()) {
            Long markId = e.getKey();
            if (!validMarkIds.contains(markId)) continue;
            Double weight = sr.weightSums().get(markId);
            double conf = (weight == null || weight <= MIN_WEIGHT) ? 0.0
                    : Math.max(0.0, Math.min(1.0, e.getValue() / weight));
            double quantized = Math.round(conf * 1_000_000d) / 1_000_000d;
            topScores.add(new MarkScore(markId, quantized));
        }

        sortDeterministically(topScores);
        List<MarkScore> limited = (k > 0 && topScores.size() > k)
                ? topScores.subList(0, k) : topScores;

        Map<Long, Double> rawScores = Collections.unmodifiableMap(new TreeMap<>(sr.scores()));
        Map<Long, Double> weightSumsCopy = Collections.unmodifiableMap(new TreeMap<>(sr.weightSums()));

        if (log.isDebugEnabled()) {
            if (sr.weightAnomalies() > 0 || sr.fanOutContributionCount() > 0) {
                log.debug("Aggregation diagnostics - duplicates={}, perMarkContrib={}, perMarkDecayApplied={}, fanOutContribCount={}, weightAnomalies={}",
                        duplicates, sr.perMarkContributions(), sr.perMarkDecayApplied(),
                        sr.fanOutContributionCount(), sr.weightAnomalies());
            }
        }

        boolean anyWeight = weightSumsCopy.values().stream()
                .anyMatch(w -> Double.isFinite(w) && w > MIN_WEIGHT);
        if (!anyWeight && !rawScores.isEmpty()) {
            log.warn("All aggregated weights are below {} ({} marks) — final confidences will be 0.",
                    MIN_WEIGHT, rawScores.size());
        }

        return new AggregationResult(limited, duplicates, sr.perMarkContributions(),
                sr.perMarkDecayApplied(), sr.fanOutContributionCount(),
                rawScores, weightSumsCopy, missingMarkMappings, sr.weightAnomalies());
    }

    // ── static sorting helpers ──────────────────────────────────────────

    public static void sortDeterministically(List<MarkScore> topScores) {
        topScores.sort((a, b) -> {
            int cmp = Double.compare(b.confidence(), a.confidence());
            if (cmp != 0) return cmp;
            if (a.markId() == null && b.markId() == null) return 0;
            if (a.markId() == null) return 1;
            if (b.markId() == null) return -1;
            return a.markId().compareTo(b.markId());
        });
    }

    static void sortGroupsEvidencesDeterministically(List<CandidateEvidence> list) {
        list.sort((a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
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
    }
}
