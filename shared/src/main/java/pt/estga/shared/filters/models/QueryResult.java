package pt.estga.shared.filters.models;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Encapsulates the result of a query processing pipeline.
 *
 * @param <T> The type of the entity being queried.
 */
public record QueryResult<T>(Specification<T> specification, Pageable pageable) {

    /**
     * Constructs a QueryResult with the given Specification and Pageable.
     *
     * @param specification The Specification for filtering.
     * @param pageable      The Pageable for pagination.
     */
    public QueryResult {
    }

    /**
     * Gets the Specification for filtering.
     *
     * @return The Specification.
     */
    @Override
    public Specification<T> specification() {
        return specification;
    }

    /**
     * Gets the Pageable for pagination.
     *
     * @return The Pageable.
     */
    @Override
    public Pageable pageable() {
        return pageable;
    }
}
