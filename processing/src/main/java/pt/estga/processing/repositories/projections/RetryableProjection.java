package pt.estga.processing.repositories.projections;

import java.time.Instant;
import java.util.UUID;

public interface RetryableProjection {
    UUID getId();
    UUID getSubmissionId();
    int getRetryCount();
    Instant getLastRetryAt();
}
