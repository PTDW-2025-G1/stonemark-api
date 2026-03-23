package pt.estga.file.services.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.events.MediaUploadedEvent;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.StorageProvider;
import pt.estga.file.services.MediaContentService;
import pt.estga.file.services.metadata.MediaMetadataService;
import pt.estga.file.services.naming.FileNamingService;
import pt.estga.file.services.naming.StoragePathStrategy;
import pt.estga.file.util.CountingInputStream;
import pt.estga.sharedweb.exceptions.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaMetadataService mediaMetadataService;
    private final MediaContentService mediaContentService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final StoragePathStrategy storagePathStrategy;
    private final FileNamingService fileNamingService;

    @Value("${storage.provider:local}")
    private String storageProvider;

    @Transactional
    public MediaFile save(InputStream fileStream, String originalFilename) throws IOException {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Create initial record with 0 size
        MediaFile media = createInitialMediaFile(originalFilename);
        media = mediaMetadataService.saveMetadata(media);

        String newFilename = fileNamingService.generateStoredFilename(media, originalFilename);
        media.setFilename(newFilename);

        // Use the strategy to generate the path
        String relativePath = storagePathStrategy.generatePath(media);

        CountingInputStream countingStream = new CountingInputStream(fileStream);
        
        // Delegate content storage to MediaContentService
        String storagePath = mediaContentService.saveContent(countingStream, relativePath);

        media.setStoragePath(storagePath);
        media.setSize(countingStream.getCount());
        media.setStatus(MediaStatus.UPLOADED);
        
        MediaFile savedMedia = mediaMetadataService.saveMetadata(media);

        applicationEventPublisher.publishEvent(
            new MediaUploadedEvent(savedMedia.getId())
        );

        return savedMedia;
    }

    private MediaFile createInitialMediaFile(String originalFilename) {
        return MediaFile.builder()
                .filename(originalFilename)
                .originalFilename(originalFilename)
                .size(0L)
                .storageProvider(StorageProvider.valueOf(storageProvider.toUpperCase()))
                .storagePath("")
                .status(MediaStatus.PROCESSING)
                .build();
    }

    public Resource loadFileById(Long fileId) {
        log.info("Loading file with ID: {}", fileId);
        MediaFile mediaFile = mediaMetadataService.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("MediaFile not found with id: " + fileId));
        return loadFile(mediaFile);
    }

    public Resource loadFile(MediaFile mediaFile) {
        if (mediaFile.getStoragePath() == null || mediaFile.getStoragePath().isEmpty()) {
            throw new FileNotFoundException("Media file has no storage path");
        }
        return mediaContentService.loadContent(mediaFile.getStoragePath());
    }

    public Optional<MediaFile> findById(Long id) {
        return mediaMetadataService.findById(id);
    }
}
