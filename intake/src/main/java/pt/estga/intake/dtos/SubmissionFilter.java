package pt.estga.intake.dtos;

import java.util.UUID;

import pt.estga.intake.enums.SubmissionSource;
import pt.estga.intake.enums.SubmissionStatus;

import java.time.Instant;

public record SubmissionFilter(
        SubmissionStatus status,
        SubmissionSource source,
        UUID submittedById,
        Instant submittedAfter,
        Instant submittedBefore
) {}
