package pt.estga.shared.filters;

import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import pt.estga.shared.filters.enums.SortDirection;

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

    /**
     * Convert this PagedRequest into a Spring Data Pageable. Defaults are
     * applied when page/size are null. SortCriteria entries are applied in order.
     */
    public Pageable toPageable() {
        int p = page != null && page >= 0 ? page : 0;
        int s = size != null && size > 0 ? Math.min(size, 500) : 20; // guard against excessive page sizes
        if (sort == null || sort.isEmpty()) return PageRequest.of(p, s);
        Sort springSort = Sort.unsorted();
        for (SortCriteria sc : sort) {
            if (sc == null || sc.getField() == null || sc.getField().isBlank()) continue;
            Sort.Order order = sc.getDirection() == null || SortDirection.ASC.equals(sc.getDirection())
                    ? Sort.Order.asc(sc.getField())
                    : Sort.Order.desc(sc.getField());
            springSort = springSort.isUnsorted() ? Sort.by(order) : springSort.and(Sort.by(order));
        }
        return PageRequest.of(p, s, springSort);
    }
}
