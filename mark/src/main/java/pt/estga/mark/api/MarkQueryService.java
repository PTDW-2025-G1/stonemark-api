package pt.estga.mark.api;

import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarkQueryService {

    Optional<MarkDto> findMarkById(Long id);

    Optional<MarkOccurrenceDto> findOccurrenceById(Long id);

    List<MarkEvidenceDto> findEvidenceByIds(List<UUID> ids);

    List<MarkDto> findDraftMarks();

    MarkDto createMark(String title);

    /**
     * Returns the occurrence of the given mark at the given place, creating it when
     * none exists yet. A {@code monumentId} identifies the place when it is known
     * and is deduplicated per mark; otherwise the coordinates are recorded on a new
     * occurrence (both may be absent, which is a valid unlocated occurrence).
     */
    MarkOccurrenceDto findOrCreateOccurrence(Long markId, Long monumentId, Double latitude, Double longitude);

    void linkFileToOccurrence(UUID fileId, Long occurrenceId, Long submissionId, float[] embedding);
}
