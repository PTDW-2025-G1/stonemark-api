package pt.estga.shared.filters.mappers;

import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Utility class for mapping client fields to entity fields.
 * Delegates the mapping logic to the injected FieldMapper.
 */
@Component
public class FilterFieldMapper {

    private final FieldMapper fieldMapper;

    /**
     * Constructs a FilterFieldMapper with the given FieldMapper.
     *
     * @param fieldMapper the FieldMapper to use for field mapping
     */
    public FilterFieldMapper(FieldMapper fieldMapper) {
        this.fieldMapper = Objects.requireNonNull(fieldMapper, "FieldMapper cannot be null");
    }

    /**
     * Maps a client field name to the corresponding entity field name.
     *
     * @param clientField the client field name
     * @return the mapped entity field name
     */
    public String map(String clientField) {
        return fieldMapper.map(clientField);
    }

    /**
     * Determines if a field is allowed based on the specific mapping logic.
     *
     * @param field the field name to check
     * @return true if the field is allowed, false otherwise
     */
    public boolean isFieldAllowed(String field) {
        return true; // Placeholder logic, adjust as needed for specific mappers.
    }
}
