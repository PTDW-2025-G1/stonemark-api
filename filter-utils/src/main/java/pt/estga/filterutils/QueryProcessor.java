package pt.estga.filterutils;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import pt.estga.filterutils.models.PagedRequest;
import pt.estga.filterutils.models.QueryResult;

/**
 * QueryProcessor is responsible for orchestrating the query pipeline.
 * It ensures consistent normalization and specification building.
 *
 * @param <T> The type of the entity being queried.
 */
public class QueryProcessor<T> {

    private final SpecificationBuilder<T> builder;

    /**
     * Constructs a QueryProcessor with the given SpecificationBuilder.
     *
     * @param builder The SpecificationBuilder for building specifications.
     */
    public QueryProcessor(SpecificationBuilder<T> builder) {
        this.builder = builder;
    }

    /**
     * Processes a PagedRequest to produce a QueryResult.
     *
     * @param request The PagedRequest containing filter, sort, and pagination data.
     * @return A QueryResult containing the Specification and Pageable.
     */
    public QueryResult<T> process(PagedRequest request) {
        // Build the specification directly from the provided filter
        Specification<T> spec = builder.build(request.getFilter());

        // Normalize the sort
        Sort sort = SortNormalizer.normalize(request.getSort());

        // Convert to Pageable
        Pageable pageable = request.toPageable(sort);

        return new QueryResult<>(spec, pageable);
    }
}
