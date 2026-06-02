package pt.estga.file.services.upload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaValidationServiceTest {

    private final MediaValidationService service = new MediaValidationService();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should accept allowed MIME types")
    void shouldAcceptAllowedTypes() throws Exception {
        Path jpeg = tempDir.resolve("test.jpg");
        Files.write(jpeg, new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00 });

        assertTrue(service.isAllowedImage(jpeg, Set.of("image/jpeg")));
    }

    @Test
    @DisplayName("should reject unsupported MIME types")
    void shouldRejectUnsupportedTypes() throws Exception {
        Path text = tempDir.resolve("test.txt");
        Files.writeString(text, "plain text content");

        assertFalse(service.isAllowedImage(text, Set.of("image/jpeg", "image/png")));
    }

    @Test
    @DisplayName("should handle empty allowed types set")
    void shouldHandleEmptyAllowedTypes() throws Exception {
        Path jpeg = tempDir.resolve("test.jpg");
        Files.write(jpeg, new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });

        assertFalse(service.isAllowedImage(jpeg, Set.of()));
    }
}
