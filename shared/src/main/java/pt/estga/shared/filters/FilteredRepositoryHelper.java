package pt.estga.shared.filters;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Helper utilities for repositories that support Specifications. Keeps mapping
 * from PagedRequest -> Pageable in a single place so consumers can call
 * FilteredRepositoryHelper.findAll(repo, spec, pagedRequest).
 */
public final class FilteredRepositoryHelper {

    private FilteredRepositoryHelper() {}

    public static <T> Page<T> findAll(JpaSpecificationExecutor<T> repo, Specification<T> spec, PagedRequest pagedRequest) {
        Pageable pageable = pagedRequest == null ? Pageable.unpaged() : pagedRequest.toPageable();
        return repo.findAll(spec, pageable);
    }

}
