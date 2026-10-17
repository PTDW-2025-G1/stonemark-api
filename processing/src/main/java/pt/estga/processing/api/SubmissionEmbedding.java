package pt.estga.processing.api;

import java.util.UUID;

/**
 * A submission's Vision embedding, used by callers that group similar submissions.
 */
public record SubmissionEmbedding(UUID submissionId, float[] embedding) {
}
