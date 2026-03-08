package pt.estga.submission.dtos;

import jakarta.validation.constraints.NotNull;
import pt.estga.submission.enums.SubmissionSource;

public record MarkOccurrenceProposalCreateDto(
        @NotNull Double latitude,
        @NotNull Double longitude,
        String userNotes,
        @NotNull SubmissionSource submissionSource,
        Long existingMonumentId,
        Long existingMarkId,
        Long photoId
) { }
