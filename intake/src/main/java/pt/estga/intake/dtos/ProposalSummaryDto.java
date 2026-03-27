package pt.estga.intake.dtos;

import pt.estga.intake.enums.SubmissionStatus;

import java.time.Instant;

public record ProposalSummaryDto(
        Long id,
        String title,
        String type,
        Long photoId,
        SubmissionStatus status,
        Instant submittedAt
) {}
