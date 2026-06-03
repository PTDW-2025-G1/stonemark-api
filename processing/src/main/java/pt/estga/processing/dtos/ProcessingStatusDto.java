package pt.estga.processing.dtos;

import pt.estga.processing.enums.ProcessingStatus;

import java.time.Instant;
import java.util.UUID;

public record ProcessingStatusDto(
        UUID processingId,
        Long submissionId,
        ProcessingStatus status,
        Instant processedAt,
        Instant failedAt,
        Instant lastRetryAt,
        int retryCount,
        int maxRetries,
        boolean permanent,
        String errorMessage
) {}
