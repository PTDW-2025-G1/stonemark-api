package pt.estga.shared.filters.utils;

import org.springframework.data.domain.Sort;
import pt.estga.shared.filters.enums.SortDirection;
import pt.estga.shared.filters.mappers.FieldMapper;
import pt.estga.shared.filters.models.SortCriteria;

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
     * @param mapper The FieldMapper to use for field mapping. Must not be null.
     *               Note: {@code mapper.map()} may throw exceptions for invalid fields.
     * @param sortCriteriaList The list of SortCriteria to normalize.
     * @return A Spring Sort object representing the normalized criteria.
     * @throws NullPointerException if {@code mapper} is null.
     * @throws IllegalArgumentException if any SortCriteria is null or contains invalid data.
     */
    public static Sort normalize(FieldMapper mapper, List<SortCriteria> sortCriteriaList) {
        Objects.requireNonNull(mapper, "FieldMapper cannot be null");

        if (sortCriteriaList == null || sortCriteriaList.isEmpty()) {
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

                String mappedField = mapper.map(sc.field());
                SortDirection direction = sc.direction() != null ? sc.direction() : SortDirection.ASC;
                return SortDirection.ASC.equals(direction)
                        ? Sort.Order.asc(mappedField)
                        : Sort.Order.desc(mappedField);
            })
            .collect(Collectors.toList());

        return Sort.by(orders);
    }
}
