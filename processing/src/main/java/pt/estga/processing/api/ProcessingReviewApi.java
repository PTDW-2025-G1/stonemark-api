package pt.estga.processing.api;

import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.shared.core.models.EmbeddingVector;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public contract the review module uses to inspect processing results and mark
 * them reviewed, without reaching into processing's repositories or entities.
 */
public interface ProcessingReviewApi {

    Optional<ProcessingOverview> findOverview(UUID submissionId);

    List<MarkSuggestionDto> findSuggestions(UUID processingId);

    long countSuggestions(UUID processingId);

    Double maxSuggestionConfidence(UUID processingId);

    /** The submission's embedding with its model provenance, when it was processed successfully. */
    Optional<EmbeddingVector> findEmbedding(UUID submissionId);

    /** Embeddings of the given submissions, for the ones that have one. */
    List<SubmissionEmbedding> findEmbeddings(Collection<UUID> submissionIds);

    void markReviewed(UUID submissionId);

    List<ProcessingModerationView> findProcessingByConfidence(double min, double max);
}
