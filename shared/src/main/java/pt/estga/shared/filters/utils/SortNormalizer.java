package pt.estga.shared.filters.utils;

import org.springframework.data.domain.Sort;
import pt.estga.shared.filters.enums.SortDirection;
import pt.estga.shared.filters.mappers.FieldMapper;
import pt.estga.shared.filters.models.SortCriteria;

import java.util.List;

/**
 * Utility class for normalizing sort criteria into Spring's Sort objects.
 */
public class SortNormalizer {

    /**
     * Converts a list of SortCriteria into a Spring Sort object.
     *
     * @param mapper The FieldMapper to use for field mapping.
     * @param sortCriteriaList The list of SortCriteria to normalize.
     * @return A Spring Sort object representing the normalized criteria.
     */
    public static Sort normalize(FieldMapper mapper, List<SortCriteria> sortCriteriaList) {
        if (sortCriteriaList == null || sortCriteriaList.isEmpty()) {
            return Sort.unsorted();
        }

        Sort combinedSort = Sort.unsorted();
        for (SortCriteria sc : sortCriteriaList) {
            if (sc == null) {
                throw new IllegalArgumentException("SortCriteria cannot be null");
            }
            if (sc.getField() == null || sc.getField().isBlank()) {
                throw new IllegalArgumentException("Sort field cannot be blank");
            }

            // Map the field name using the provided FieldMapper
            String mappedField = mapper.map(sc.getField());

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
