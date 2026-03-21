package pt.estga.filterutils.utils;

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
     * @throws NullPointerException if {@code mapper} is null.
     * @throws IllegalArgumentException if any SortCriteria is null or contains invalid data.
     */
    public static Sort normalize(List<SortCriteria> sortCriteriaList) {
        Objects.requireNonNull(sortCriteriaList, "Sort criteria list cannot be null");

        if (sortCriteriaList.isEmpty()) {
            return Sort.unsorted();
        }

        // Collect Sort.Orders in one pass
        List<Sort.Order> orders = sortCriteriaList.stream()
            .map(sc -> {
                if (sc == null) {
                    throw new IllegalArgumentException("SortCriteria cannot be null");
                }
                if (sc.field() == null || sc.field().isBlank()) {
                    throw new IllegalArgumentException("Sort field cannot be blank");
                }

                String mappedField = sc.field();
                SortDirection direction = sc.direction() != null ? sc.direction() : SortDirection.ASC;
                return SortDirection.ASC.equals(direction)
                        ? Sort.Order.asc(mappedField)
                        : Sort.Order.desc(mappedField);
            })
            .collect(Collectors.toList());

        return Sort.by(orders);
    }
}
