package pt.estga.mark.api;

import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.shared.core.models.EmbeddingVector;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarkQueryService {

    Optional<MarkDto> findMarkById(UUID id);

    List<MarkDto> findDraftMarks();

    /**
     * Returns the published, non-deleted marks with the given ids, for callers that
     * resolve several marks at once (for example bookmarks).
     */
    List<MarkDto> findPublishedMarksByIds(Collection<UUID> ids);

    /**
     * Returns the published, non-deleted occurrences with the given ids, for callers
     * that resolve several occurrences at once (for example bookmarks).
     */
    List<MarkOccurrenceDto> findPublishedOccurrencesByIds(Collection<UUID> ids);

    MarkDto createMark(String title, String description);

    /**
     * Returns the occurrence of the given mark at the given place, creating it when
     * none exists yet. A {@code monumentId} identifies the place when it is known
     * and is deduplicated per mark; otherwise coordinates within a small radius of an
     * existing occurrence of the same mark are treated as that occurrence, and both
     * place references may be absent (a valid unlocated occurrence).
     */
    MarkOccurrenceDto findOrCreateOccurrence(UUID markId, UUID monumentId, Double latitude, Double longitude);

    /** Publishes an occurrence that is still a draft, so approvals become visible. */
    void publishOccurrence(UUID occurrenceId);

    /**
     * Attaches the submission's photo, and its versioned embedding when present, to an
     * occurrence as evidence. The embedding provenance is stored so a later model swap
     * can find and recompute stale vectors.
     */
    void linkFileToOccurrence(UUID fileId, UUID occurrenceId, UUID submissionId, EmbeddingVector embedding);
}
