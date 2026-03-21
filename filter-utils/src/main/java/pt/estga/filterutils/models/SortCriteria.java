package pt.estga.filterutils.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import pt.estga.filterutils.enums.SortDirection;

/**
 * Represents sorting criteria for a query.
 *
 * @param field     The field to sort by.
 * @param direction The direction of sorting (ASC or DESC). Defaults to ASC.
 */
@Builder
@JsonDeserialize(using = SortCriteria.SortCriteriaDeserializer.class)
public record SortCriteria(String field, SortDirection direction) {
    public SortCriteria {
        if (direction == null) {
            direction = SortDirection.ASC;
        }
    }

    /**
     * Custom deserializer to ensure default value for direction.
     */
    static class SortCriteriaDeserializer extends JsonDeserializer<SortCriteria> {
        @Override
        public SortCriteria deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            String field = node.get("field").asText();
            SortDirection direction;
            try {
                direction = node.has("direction") && !node.get("direction").isNull()
                        ? SortDirection.valueOf(node.get("direction").asText().toUpperCase())
                        : SortDirection.ASC;
            } catch (IllegalArgumentException e) {
                direction = SortDirection.ASC; // Default to ASC if invalid value is provided
            }
            return new SortCriteria(field, direction);
        }
    }
}
