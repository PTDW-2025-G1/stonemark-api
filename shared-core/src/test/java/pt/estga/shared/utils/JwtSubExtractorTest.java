package pt.estga.shared.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSubExtractorTest {

    @Test
    void extractSub_shouldReturnSub_whenPresent() {
        String sub = JwtSubExtractor.extractSub(Map.of("sub", "abc-123"));

        assertEquals("abc-123", sub);
    }

    @Test
    void extractSub_shouldThrow_whenClaimsNull() {
        assertThrows(IllegalArgumentException.class, () -> JwtSubExtractor.extractSub(null));
    }

    @Test
    void extractSub_shouldThrow_whenSubMissing() {
        assertThrows(IllegalArgumentException.class, () -> JwtSubExtractor.extractSub(Map.of("iss", "x")));
    }

    @Test
    void extractSub_shouldThrow_whenSubBlank() {
        assertThrows(IllegalArgumentException.class, () -> JwtSubExtractor.extractSub(Map.of("sub", " ")));
    }
}
