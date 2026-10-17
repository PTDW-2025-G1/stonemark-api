package pt.estga.processing.api;

/**
 * A submission's Vision embedding, used by callers that group similar submissions.
 */
public record SubmissionEmbedding(Long submissionId, float[] embedding) {
}
