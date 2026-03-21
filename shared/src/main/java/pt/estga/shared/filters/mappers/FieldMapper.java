package pt.estga.shared.filters.mappers;

/**
 * Interface for mapping client fields to entity fields.
 * Allows for entity-specific mapping logic to be injected dynamically.
 */
public interface FieldMapper {

    /**
     * Maps a client field name to the corresponding entity field name.
     *
     * @param field the client field name
     * @return the mapped entity field name
     * @throws IllegalArgumentException if the field is not allowed
     */
    String map(String field);

    /**
     * Checks if a given field is allowed for mapping.
     *
     * @param field the field to check
     * @return true if the field is allowed, false otherwise
     */
    boolean isFieldAllowed(String field);
}
