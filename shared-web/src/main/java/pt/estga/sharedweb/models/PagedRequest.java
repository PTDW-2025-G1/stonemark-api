package pt.estga.sharedweb.models;

import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
     * Converts the current PagedRequest into a Pageable object.
     *
     * @param sort The sort to apply.
     * @return A Pageable object representing the pagination and sorting.
     */
    public Pageable toPageable(Sort sort) {
        int effectivePage = (page == null || page < 0) ? DEFAULT_PAGE : page;
        int effectiveSize = (size == null || size < 1 || size > MAX_SIZE) ? DEFAULT_SIZE : size;
        return PageRequest.of(effectivePage, effectiveSize, sort);
    }
}
