package pt.estga.sharedweb.models;

import jakarta.persistence.criteria.JoinType;
import lombok.*;
import pt.estga.sharedweb.enums.FilterOperator;
import pt.estga.sharedweb.enums.LikeMode;

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
@Getter
@Builder
public class FilterCriteria {
    private final String field;
    private final FilterOperator operator;
    private final Object value;
    @Builder.Default
    private final LikeMode likeMode = LikeMode.CONTAINS;
    private final boolean caseSensitive;
    @Builder.Default
    private final JoinType joinType = JoinType.LEFT;
}
