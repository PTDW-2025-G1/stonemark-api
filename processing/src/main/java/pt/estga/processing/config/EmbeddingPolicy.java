package pt.estga.processing.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Embedding-related policy extracted from configuration. Provides a single
 * place to read embedding dimension or other embedding validation options.
 */
@Getter
@Component
public class EmbeddingPolicy {

    private final int dimension;

    public EmbeddingPolicy(@Value("${processing.embedding.dimension:0}") int dimension) {
        this.dimension = dimension;
    }
}
