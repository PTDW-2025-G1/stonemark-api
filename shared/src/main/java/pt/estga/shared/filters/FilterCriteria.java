package pt.estga.shared.filters;

import lombok.*;
import pt.estga.shared.filters.enums.FilterOperator;
import pt.estga.shared.filters.enums.LikeMode;

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
     */
    private LikeMode likeMode = LikeMode.CONTAINS;
    /**
     * Controls case sensitivity for LIKE operator. If true, LIKE is case-sensitive and preserves index usage.
     */
    private boolean caseSensitive = false;

}
