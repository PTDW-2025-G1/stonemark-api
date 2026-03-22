package pt.estga.sharedweb.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import pt.estga.sharedweb.enums.SortDirection;

/**
 * Represents sorting criteria for a query.
 *
 * @param field     The field to sort by.
 * @param direction The direction of sorting (ASC or DESC). Defaults to ASC.
 */
@Slf4j
@Builder
public record SortCriteria(String field, SortDirection direction) {
    public SortCriteria {
        if (direction == null) {
            direction = SortDirection.ASC;
        }
    }

    @JsonCreator
    public static SortCriteria create(@JsonProperty("field") String field,
                                      @JsonProperty("direction") String direction) {
        SortDirection dir = SortDirection.ASC;
        if (direction != null && !direction.isBlank()) {
            try {
                dir = SortDirection.valueOf(direction.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                log.warn("Invalid sort direction '{}', defaulting to ASC", direction);
            }
        }
        return new SortCriteria(field, dir);
    }
}
