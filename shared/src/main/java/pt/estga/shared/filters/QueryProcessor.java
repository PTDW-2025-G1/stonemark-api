package pt.estga.shared.filters;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import pt.estga.shared.filters.mappers.FieldMapper;
import pt.estga.shared.filters.models.PagedRequest;
import pt.estga.shared.filters.models.QueryResult;
import pt.estga.shared.filters.utils.FilterNormalizer;
import pt.estga.shared.filters.utils.SortNormalizer;

/**
 * QueryProcessor is responsible for orchestrating the query pipeline.
 * It ensures consistent normalization and specification building.
 *
 * @param <T> The type of the entity being queried.
 */
public class QueryProcessor<T> {

    private final FieldMapper mapper;
    private final SpecificationBuilder<T> builder;

    /**
     * Constructs a QueryProcessor with the given FieldMapper and SpecificationBuilder.
     *
     * @param mapper  The FieldMapper for field normalization.
     * @param builder The SpecificationBuilder for building specifications.
     */
    public QueryProcessor(FieldMapper mapper, SpecificationBuilder<T> builder) {
        this.mapper = mapper;
        this.builder = builder;
    }

    /**
     * Processes a PagedRequest to produce a QueryResult.
     *
     * @param request The PagedRequest containing filter, sort, and pagination data.
     * @return A QueryResult containing the Specification and Pageable.
     */
    public QueryResult<T> process(PagedRequest request) {
        // Normalize the filter
        var normalizedFilter = FilterNormalizer.normalize(request.getFilter());

        // Build the specification
        Specification<T> spec = builder.build(normalizedFilter);

        // Normalize the sort
        Sort sort = SortNormalizer.normalize(mapper, request.getSort());

        // Convert to Pageable
        Pageable pageable = request.toPageable(sort);

        return new QueryResult<>(spec, pageable);
    }
}
