package pt.estga.sharedweb.models;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Encapsulates the result of a query processing pipeline.
 *
 * @param <T> The type of the entity being queried.
 */
public record QueryResult<T>(Specification<T> specification, Pageable pageable) {
}
