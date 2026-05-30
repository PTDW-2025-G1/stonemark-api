package pt.estga.file.services.naming;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.enums.StorageProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoragePathStrategyTest {

    private final StoragePathStrategy strategy = new StoragePathStrategy();

    @Test
    @DisplayName("should generate path with two-level sharding from UUID")
    void shouldGenerateShardedPath() {
        var media = mediaFileWithFilename("550e8400-e29b-41d4-a716-446655440000.jpg");

        String path = strategy.generatePath(media);

        assertEquals("55/0e/550e8400-e29b-41d4-a716-446655440000.jpg", path);
    }

    @Test
    @DisplayName("should handle filename without extension")
    void shouldHandleFilenameWithoutExtension() {
        var media = mediaFileWithFilename("550e8400-e29b-41d4-a716-446655440000");

        String path = strategy.generatePath(media);

        assertEquals("55/0e/550e8400-e29b-41d4-a716-446655440000", path);
    }

    @Test
    @DisplayName("should throw on null filename")
    void shouldThrowOnNullFilename() {
        var media = mediaFileWithFilename(null);

        assertThrows(IllegalArgumentException.class, () -> strategy.generatePath(media));
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
