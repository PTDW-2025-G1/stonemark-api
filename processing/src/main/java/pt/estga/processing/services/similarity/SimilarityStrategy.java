package pt.estga.processing.services.similarity;

import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;

import java.util.List;

/**
 * Produces candidate matches for a processed submission.
 *
 * <p>This is the seam that keeps matching swappable: the current implementation ranks
 * candidates with pgvector cosine similarity, while classic shape descriptors, a
 * vision-language re-ranker, or a fine-tuned embedding model can be dropped in later
 * without touching the processing pipeline. Implementations return suggestions only;
 * they never create marks or make decisions.
 */
public interface SimilarityStrategy {

    List<MarkSuggestion> findSimilar(MarkEvidenceProcessing processing, int k);
}
