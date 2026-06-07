package pt.estga.file.services.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import pt.estga.file.config.StorageProperties;
import pt.estga.commonweb.exceptions.FileNotFoundException;
import pt.estga.commonweb.exceptions.FileStorageException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceLocalImplTest {

    private FileStorageServiceLocalImpl storage;

    @TempDir
    Path tempRoot;

    @BeforeEach
    void setUp() {
        var props = new StorageProperties();
        props.getLocal().setRootPath(tempRoot.toString());
        storage = new FileStorageServiceLocalImpl(props);
    }

    @Test
    @DisplayName("should store and load a file")
    void shouldStoreAndLoad() {
        String content = "file content";
        String path = storage.storeFile(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                "test/file.txt", content.length());

        Resource resource = storage.loadFile(path);

        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    @DisplayName("should delete a file")
    void shouldDelete() {
        String content = "to delete";
        String path = storage.storeFile(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                "delete/me.txt", content.length());

        storage.deleteFile(path);

        assertThrows(FileNotFoundException.class, () -> storage.loadFile(path));
    }

    @Test
    @DisplayName("should throw on loading non-existent file")
    void shouldThrowOnMissingFile() {
        assertThrows(FileNotFoundException.class,
                () -> storage.loadFile("nonexistent/file.txt"));
    }

    @Test
    @DisplayName("should reject path traversal in storeFile")
    void shouldRejectPathTraversalOnStore() {
        assertThrows(SecurityException.class,
                () -> storage.storeFile(
                        new ByteArrayInputStream("data".getBytes()),
                        "../../etc/passwd", 4));
    }

    @Test
    @DisplayName("should reject path traversal in loadFile")
    void shouldRejectPathTraversalOnLoad() {
        assertThrows(SecurityException.class,
                () -> storage.loadFile("../../etc/passwd"));
    }

    @Test
    @DisplayName("should reject path traversal in deleteFile")
    void shouldRejectPathTraversalOnDelete() {
        assertThrows(SecurityException.class,
                () -> storage.deleteFile("../../etc/passwd"));
    }

    @Test
    @DisplayName("should throw on null input stream")
    void shouldThrowOnNullStream() {
        assertThrows(FileStorageException.class,
                () -> storage.storeFile(null, "file.txt", 0));
    }
}
