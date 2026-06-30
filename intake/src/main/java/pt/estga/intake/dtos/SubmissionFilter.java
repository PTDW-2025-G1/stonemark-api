package pt.estga.intake.dtos;

import pt.estga.intake.enums.SubmissionSource;
import pt.estga.intake.enums.SubmissionStatus;

import java.time.Instant;

public record SubmissionFilter(
        SubmissionStatus status,
        SubmissionSource source,
        Long submittedById,
        Instant submittedAfter,
        Instant submittedBefore,
        String divisionCode
) {}
