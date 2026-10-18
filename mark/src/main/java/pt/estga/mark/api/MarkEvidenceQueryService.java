package pt.estga.mark.api;

import pt.estga.mark.dtos.EvidenceMarkDto;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;

import java.util.List;
import java.util.UUID;

public interface MarkEvidenceQueryService {

    /**
     * Nearest evidence by cosine similarity to the given embedding, most similar first,
     * limited to {@code k} results and restricted to similarity at or above
     * {@code minSimilarity} (0.0 - 1.0).
     */
    List<MarkEvidenceDistanceDto> findTopKSimilar(float[] embedding, int k, double minSimilarity);

    List<EvidenceMarkDto> findMarksByEvidenceIds(List<UUID> evidenceIds);
}
