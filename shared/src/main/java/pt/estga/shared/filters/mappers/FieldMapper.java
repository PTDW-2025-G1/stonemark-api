package pt.estga.shared.filters.mappers;

import java.util.Optional;

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

    /**
     * Maps a client field name to the corresponding entity field name or throws an exception if the field is not allowed.
     *
     * @param field the client field name
     * @return the mapped entity field name
     * @throws IllegalArgumentException if the field is not allowed
     */
    default String mapOrThrow(String field) {
        if (!isFieldAllowed(field)) {
            throw new IllegalArgumentException("Field not allowed: " + field);
        }
        return map(field);
    }

    /**
     * Maps a client field name to the corresponding entity field name, returning an Optional.
     *
     * @param field the client field name
     * @return an Optional containing the mapped entity field name, or an empty Optional if the field is not allowed
     */
    default Optional<String> safeMap(String field) {
        return isFieldAllowed(field) ? Optional.of(map(field)) : Optional.empty();
    }
}
