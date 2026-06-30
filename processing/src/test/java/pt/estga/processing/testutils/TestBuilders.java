package pt.estga.processing.testutils;

import java.util.UUID;

public final class TestBuilders {

    private TestBuilders() {}

    public static Long mark(Long id) {
        return id;
    }

    public static UUID uuid(String literal) {
        return UUID.fromString(literal);
    }
}
