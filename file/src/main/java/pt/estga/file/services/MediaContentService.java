package pt.estga.file.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import pt.estga.file.storage.FileStorageService;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaContentService {

    private final FileStorageService fileStorageService;

    /**
     * Result for content save operation returning storage path and number of bytes written.
     */
    public record SaveResult(String storagePath, long size) {}

    /**
     * Saves the provided stream using the underlying FileStorageService while
     * counting bytes written. Counting responsibility is owned by the content
     * service so callers don't need to wrap streams.
     */
    public SaveResult saveContent(InputStream fileStream, String filename) {
        // Wrap the stream to count bytes
        var counting = new pt.estga.file.util.CountingInputStream(fileStream);
        String storagePath = fileStorageService.storeFile(counting, filename);
        return new SaveResult(storagePath, counting.getCount());
    }

    public Resource loadContent(String storagePath) {
        return fileStorageService.loadFile(storagePath);
    }

    /**
     * Deletes the content at the given storage path using the underlying storage
     * implementation. This is used to cleanup files when metadata persistence fails.
     */
    public void deleteContent(String storagePath) {
        fileStorageService.deleteFile(storagePath);
    }
}
