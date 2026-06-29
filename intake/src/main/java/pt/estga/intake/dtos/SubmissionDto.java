package pt.estga.intake.dtos;

import pt.estga.intake.enums.SubmissionSource;
import pt.estga.intake.enums.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubmissionDto(
        Long id,
        SubmissionStatus status,
        SubmissionSource submissionSource,
        Instant submittedAt,
        Double latitude,
        Double longitude,
        String userNotes,
        UUID originalMediaFileId,
        Long submittedById,
        Long divisionId,
        String divisionName
) {}
