package pt.estga.shared.filters.mappers;

import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FieldMapper implementation for the User entity.
 */
@Component
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

    @Override
    public boolean isFieldAllowed(String field) {
        return USER_FIELDS.containsKey(field);
    }
}
