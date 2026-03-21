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
@Builder
public class FilterCriteria {

    private final String field;
    private final FilterOperator operator;
    private final Object value;
    /**
     * Specifies the LIKE mode for string filtering. Default is LikeMode.CONTAINS.
     * The builder must preserve this default; @Builder.Default ensures the value is set when using Lombok's builder.
     */
    @Builder.Default
    private final LikeMode likeMode = LikeMode.CONTAINS;
    /**
     * Controls case sensitivity for LIKE operator. If true, LIKE is case-sensitive and preserves index usage.
     */
    private final boolean caseSensitive;

    /**
     * Custom no-args constructor for frameworks like Jackson.
     * Ensures that default values are applied and the object is valid.
     */
    public FilterCriteria() {
        this.field = ""; // Default to empty string
        this.operator = FilterOperator.EQ; // Default operator
        this.value = null; // Default value
        this.likeMode = LikeMode.CONTAINS; // Default like mode
        this.caseSensitive = false; // Default case sensitivity
    }

    /**
     * Validates the required fields to ensure the object is always in a valid state.
     */
    @Builder(builderMethodName = "validatedBuilder")
    private static FilterCriteria createValidated(String field, FilterOperator operator, Object value, LikeMode likeMode, boolean caseSensitive) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("Field must not be null or empty");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Operator must not be null");
        }
        return new FilterCriteria(field, operator, value, likeMode, caseSensitive);
    }
}
