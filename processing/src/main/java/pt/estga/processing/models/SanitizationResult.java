package pt.estga.processing.models;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record SanitizationResult(
        List<CandidateEvidence> candidates,
        Set<UUID> idSet,
        java.util.Set<CandidateKey> candidateKeys,
        int rawHitCount,
        int invalidSimilarityCount,
        int outOfRangeCount
) {
}
