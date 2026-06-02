package pt.estga.processing.repositories.projections;

import java.time.Instant;
import java.util.UUID;

public interface RetryableProjection {
    UUID getId();
    Long getSubmissionId();
    int getRetryCount();
    Instant getLastRetryAt();
}
