package pt.estga.submission.dtos;

import pt.estga.submission.enums.SubmissionStatus;

import java.time.Instant;

public record MarkOccurrenceProposalListDto(
        Long id,
        String title,
        Long photoId,
        SubmissionStatus status,
        Instant submittedAt
) {}
