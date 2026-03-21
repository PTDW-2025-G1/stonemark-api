package pt.estga.shared.filters.models;

import lombok.*;
import pt.estga.shared.filters.enums.FilterOperator;
import pt.estga.shared.filters.enums.LikeMode;

/**
 * Represents a single filtering condition used by the filtering subsystem.
 * <p>
 * Expected JSON shape when received from clients:
 * {
 *   "field": "entityProperty",
 *   "operator": "EQ|LIKE|IN|BETWEEN|...",
 *   "value": <single value or array for IN/BETWEEN>,
 *   "likeMode": "CONTAINS|STARTS_WITH|ENDS_WITH", // optional
 *   "caseSensitive": false // optional
 * }
 * <p>
 * Only fields required by a specific operator must be provided (for example, IN requires a list value).
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder(builderMethodName = "validatedBuilder")
public class FilterCriteria {
    private final String field;
    private final FilterOperator operator;
    private final Object value;
    @Builder.Default
    private final LikeMode likeMode = LikeMode.CONTAINS;
    private final boolean caseSensitive;

    /**
     * Creates a validated builder for constructing FilterCriteria instances.
     * This method ensures that all required fields are properly validated before
     * the object is built.
     * <p>
     * Example usage:
     * <pre>
     *     FilterCriteria criteria = FilterCriteria.validatedBuilder()
     *         .field("name")
     *         .operator(FilterOperator.EQ)
     *         .value("John")
     *         .likeMode(LikeMode.CONTAINS)
     *         .caseSensitive(false)
     *         .build();
     * </pre>
     *
     * @return A validated builder instance for FilterCriteria.
     */
    public static FilterCriteria validatedBuilder(String field, FilterOperator operator, Object value, LikeMode likeMode, boolean caseSensitive) {
        if (field == null || field.isBlank()) throw new IllegalArgumentException("Field cannot be blank");
        if (operator == null) throw new IllegalArgumentException("Operator cannot be null");
        return new FilterCriteria(field, operator, value, likeMode, caseSensitive);
    }
}
