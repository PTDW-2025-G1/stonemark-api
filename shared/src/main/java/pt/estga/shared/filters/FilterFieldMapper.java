package pt.estga.shared.filters;

import java.util.Map;

public class FilterFieldMapper {
    private static final Map<String, String> FIELDS = Map.of(
        "email", "email",
        "role", "roles.name",
        "age", "age"
    );

    public static String map(String clientField) {
        String mapped = FIELDS.get(clientField);
        if (mapped == null) {
            throw new IllegalArgumentException("Field not allowed: " + clientField);
        }
        return mapped;
    }
}
