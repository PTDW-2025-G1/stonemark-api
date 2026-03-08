package pt.estga.submission.dtos;

import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.enums.SubmissionSource;

public record MarkOccurrenceProposalDto(
        Long id,
        Integer priority,
        Double latitude,
        Double longitude,
        boolean newMark,
        String userNotes,
        SubmissionSource submissionSource,
        SubmissionStatus status,
        Long existingMonumentId,
        String existingMonumentName,
        Long existingMarkId,
        String existingMarkName,
        Long photoId
) { }
