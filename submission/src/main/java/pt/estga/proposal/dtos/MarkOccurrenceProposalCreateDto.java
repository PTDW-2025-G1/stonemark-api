package pt.estga.proposal.dtos;

import jakarta.validation.constraints.NotNull;
import pt.estga.proposal.enums.SubmissionSource;

public record MarkOccurrenceProposalCreateDto(
        @NotNull Double latitude,
        @NotNull Double longitude,
        String userNotes,
        @NotNull SubmissionSource submissionSource,
        Long existingMonumentId,
        Long existingMarkId,
        Long photoId
) { }
