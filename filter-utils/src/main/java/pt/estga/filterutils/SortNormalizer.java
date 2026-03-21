package pt.estga.filterutils;

import org.springframework.data.domain.Sort;
import pt.estga.filterutils.enums.SortDirection;
import pt.estga.filterutils.models.SortCriteria;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class for normalizing sort criteria into Spring's Sort objects.
 */
public class SortNormalizer {

    /**
     * Converts a list of SortCriteria into a Spring Sort object.
     *
     * @param sortCriteriaList The list of SortCriteria to normalize.
     * @return A Spring Sort object representing the normalized criteria.
     * @throws IllegalArgumentException if any SortCriteria is null or contains invalid data.
     */
    public static Sort normalize(List<SortCriteria> sortCriteriaList) {
        Objects.requireNonNull(sortCriteriaList, "Sort criteria list cannot be null");

        if (sortCriteriaList.isEmpty()) {
            return Sort.unsorted();
        }

        // Collect Sort.Orders in one pass; perform point-of-use normalization (trim fields)
        List<Sort.Order> orders = sortCriteriaList.stream()
            .map(sc -> {
                if (sc == null) {
                    throw new IllegalArgumentException("SortCriteria cannot be null");
                }
                String rawField = sc.field();
                if (rawField == null || rawField.isBlank()) {
                    throw new IllegalArgumentException("Sort field cannot be blank");
                }

                // Trim only at the point of use to avoid mutating the original model
                String mappedField = rawField.trim();

                // Default to ASC when direction is missing
                SortDirection direction = sc.direction() != null ? sc.direction() : SortDirection.ASC;

                // Safe comparison with enum value
                return SortDirection.DESC.equals(direction)
                        ? Sort.Order.desc(mappedField)
                        : Sort.Order.asc(mappedField);
            })
            .collect(Collectors.toList());

        return Sort.by(orders);
    }
}
