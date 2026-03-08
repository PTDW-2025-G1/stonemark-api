package pt.estga.submission.dtos;

import pt.estga.submission.enums.SubmissionStatus;

import java.time.Instant;

public record ProposalSummaryDto(
        Long id,
        String title,
        String type,
        Long photoId,
        SubmissionStatus status,
        Instant submittedAt
) {}
