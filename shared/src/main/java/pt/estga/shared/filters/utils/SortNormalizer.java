package pt.estga.shared.filters.utils;

import org.springframework.data.domain.Sort;
import pt.estga.shared.filters.enums.SortDirection;
import pt.estga.shared.filters.mappers.FilterFieldMapper;
import pt.estga.shared.filters.models.SortCriteria;

import java.util.List;

/**
 * Utility class for normalizing sort criteria into Spring's Sort objects.
 */
public class SortNormalizer {

    /**
     * Converts a list of SortCriteria into a Spring Sort object.
     *
     * @param entityClass The entity class for field mapping.
     * @param sortCriteriaList The list of SortCriteria to normalize.
     * @return A Spring Sort object representing the normalized criteria.
     */
    public static Sort normalize(Class<?> entityClass, List<SortCriteria> sortCriteriaList) {
        if (sortCriteriaList == null || sortCriteriaList.isEmpty()) {
            return Sort.unsorted();
        }

        Sort combinedSort = Sort.unsorted();
        for (SortCriteria sc : sortCriteriaList) {
            if (sc == null || sc.getField() == null || sc.getField().isBlank()) {
                continue;
            }

            // Map the field name using FilterFieldMapper
            String mappedField = FilterFieldMapper.map(entityClass, sc.getField());

            // Determine the sort direction
            SortDirection direction = sc.getDirection() != null ? sc.getDirection() : SortDirection.ASC;
            Sort.Order order = SortDirection.ASC.equals(direction)
                    ? Sort.Order.asc(mappedField)
                    : Sort.Order.desc(mappedField);

            combinedSort = combinedSort.isUnsorted() ? Sort.by(order) : combinedSort.and(Sort.by(order));
        }

        return combinedSort;
    }
}
