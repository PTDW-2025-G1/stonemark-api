package pt.estga.shared.filters;

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
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FilterCriteria {

    private String field;
    private FilterOperator operator;
    private Object value;
    /**
     * Specifies the LIKE mode for string filtering. Default is LikeMode.CONTAINS.
     * The builder must preserve this default; @Builder.Default ensures the value is set when using Lombok's builder.
     */
    @Builder.Default
    private LikeMode likeMode = LikeMode.CONTAINS;
    /**
     * Controls case sensitivity for LIKE operator. If true, LIKE is case-sensitive and preserves index usage.
     */
    private boolean caseSensitive = false;

}
