package pt.estga.shared.filters.models;

import lombok.*;
import pt.estga.shared.filters.enums.SortDirection;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SortCriteria {
    private String field;
    private SortDirection direction;
}
