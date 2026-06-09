package pt.estga.file.services.naming;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.StorageProvider;

import static org.junit.jupiter.api.Assertions.*;

class FileNamingServiceTest {

    private final FileNamingService service = new FileNamingService();

    @Test
    @DisplayName("should generate UUID-based filename preserving extension")
    void shouldPreserveExtension() {
        String result = service.generateStoredFilename("photo.jpg");

        assertTrue(result.endsWith(".jpg"));
        assertEquals(40, result.length());
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

    @Test
    @DisplayName("should generate path with two-level sharding from UUID")
    void shouldGenerateShardedPath() {
        var media = mediaFileWithFilename("550e8400-e29b-41d4-a716-446655440000.jpg");

        String path = service.generatePath(media);

        assertEquals("55/0e/550e8400-e29b-41d4-a716-446655440000.jpg", path);
    }

    @Test
    @DisplayName("should handle filename without extension in path generation")
    void shouldHandleFilenameWithoutExtensionInPath() {
        var media = mediaFileWithFilename("550e8400-e29b-41d4-a716-446655440000");

        String path = service.generatePath(media);

        assertEquals("55/0e/550e8400-e29b-41d4-a716-446655440000", path);
    }

    @Test
    @DisplayName("should throw on null filename in path generation")
    void shouldThrowOnNullFilename() {
        var media = mediaFileWithFilename(null);

        assertThrows(IllegalArgumentException.class, () -> service.generatePath(media));
    }

    private MediaFile mediaFileWithFilename(String filename) {
        return MediaFile.builder()
                .filename(filename)
                .storageProvider(StorageProvider.LOCAL)
                .storagePath("")
                .status(MediaStatus.PROCESSING)
                .build();
    }
}
