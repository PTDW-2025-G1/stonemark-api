package pt.estga.shared.filters;

import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import pt.estga.shared.filters.enums.SortDirection;
import pt.estga.shared.filters.models.FilterNode;
import pt.estga.shared.filters.models.SortCriteria;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PagedRequest {
    private FilterNode filter;
    private Integer page;
    private Integer size;
    private List<SortCriteria> sort;

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
    public Pageable toPageable() {
        int p = page != null && page >= 0 ? page : DEFAULT_PAGE;
        int s = size != null && size > 0 ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE; // guard against excessive page sizes
        if (sort == null || sort.isEmpty()) return PageRequest.of(p, s);

        Sort springSort = Sort.unsorted();
        for (SortCriteria sc : sort) {
            if (sc == null || sc.getField() == null || sc.getField().isBlank()) continue;

            // Validate and map the field using FilterFieldMapper
            String mappedField = FilterFieldMapper.map(sc.getField());

            SortDirection direction = sc.getDirection() != null ? sc.getDirection() : SortDirection.ASC;
            Sort.Order order = SortDirection.ASC.equals(direction)
                    ? Sort.Order.asc(mappedField)
                    : Sort.Order.desc(mappedField);
            springSort = springSort.isUnsorted() ? Sort.by(order) : springSort.and(Sort.by(order));
        }
        return PageRequest.of(p, s, springSort);
    }
}
