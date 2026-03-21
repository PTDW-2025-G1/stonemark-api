package pt.estga.shared.filters.models;

import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import pt.estga.shared.filters.utils.SortNormalizer;

import java.util.List;

@Value
@Builder
public class PagedRequest {
    FilterNode filter;
    Integer page;
    Integer size;
    List<SortCriteria> sort;

    // Default paging values and maximums are centralized for ease of maintenance and testing.
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 500;

    /**
     * Convert this PagedRequest into a Spring Data Pageable. Defaults are
     * applied when page/size are null. SortCriteria entries are applied in order.
     * <p>
     * Expected JSON shape when received from clients:
     * {
     *   "filter": { ... },
     *   "page": 0,
     *   "size": 20,
     *   "sort": [{ "field": "name", "direction": "ASC" }]
     * }
     */
    public Pageable toPageable(Class<?> entityClass) {
        int p = page != null && page >= 0 ? page : DEFAULT_PAGE;
        int s = size != null && size > 0 ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE; // guard against excessive page sizes
        if (sort == null || sort.isEmpty()) return PageRequest.of(p, s);

        Sort springSort = SortNormalizer.normalize(entityClass, sort);
        return PageRequest.of(p, s, springSort);
    }
}
