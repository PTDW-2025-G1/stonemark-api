package pt.estga.file.services.naming;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileNamingServiceTest {

    private final FileNamingService service = new FileNamingService();

    @Test
    @DisplayName("should generate UUID-based filename preserving extension")
    void shouldPreserveExtension() {
        String result = service.generateStoredFilename("photo.jpg");

        assertTrue(result.endsWith(".jpg"));
        assertEquals(40, result.length()); // 36 UUID chars + ".jpg"
    }

    @Test
    @DisplayName("should handle filename without extension")
    void shouldHandleNoExtension() {
        String result = service.generateStoredFilename("photo");

        assertEquals(36, result.length());
        assertFalse(result.contains("."));
    }

    @Test
    @DisplayName("should handle null filename gracefully")
    void shouldHandleNullFilename() {
        String result = service.generateStoredFilename(null);

        assertEquals(36, result.length());
        assertFalse(result.contains("."));
    }

    @Test
    @DisplayName("should generate unique filenames on each call")
    void shouldGenerateUniqueFilenames() {
        String name1 = service.generateStoredFilename("a.jpg");
        String name2 = service.generateStoredFilename("a.jpg");

        assertNotEquals(name1, name2);
    }
}
