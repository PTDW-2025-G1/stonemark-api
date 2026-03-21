package pt.estga.shared.filters.mappers;

import java.util.Map;

/**
 * FieldMapper implementation for the User entity.
 */
public class UserFieldMapper implements FieldMapper {

    private static final Map<String, String> USER_FIELDS = Map.of(
        "email", "email",
        "role", "roles.name",
        "age", "age"
    );

    @Override
    public String map(String field) {
        String mapped = USER_FIELDS.get(field);
        if (mapped == null) {
            throw new IllegalArgumentException("Field not allowed: " + field);
        }
        return mapped;
    }
}
