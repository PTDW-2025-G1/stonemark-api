package pt.estga.file.services.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.events.MediaUploadedEvent;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.StorageProvider;
import pt.estga.file.services.MediaContentService;
import pt.estga.file.services.MediaMetadataService;
import pt.estga.file.services.MediaService;
import pt.estga.file.services.StoragePathStrategy;
import pt.estga.shared.exceptions.FileNotFoundException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final MediaMetadataService mediaMetadataService;
    private final MediaContentService mediaContentService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final StoragePathStrategy storagePathStrategy;
    private final Tika tika = new Tika();

    @Value("${storage.provider:local}")
    private String storageProvider;

    @Override
    @Transactional
    public MediaFile save(InputStream fileStream, String originalFilename) throws IOException {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Create initial record with 0 size
        MediaFile media = createInitialMediaFile(originalFilename);
        media = mediaMetadataService.saveMetadata(media);

        String extension = StringUtils.getFilenameExtension(originalFilename);
        String newFilename = "stonemark-" + media.getId() + (extension != null ? "." + extension : "");
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

    @Override
    public Resource loadFileById(Long fileId) {
        log.info("Loading file with ID: {}", fileId);
        MediaFile mediaFile = mediaMetadataService.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("MediaFile not found with id: " + fileId));
        return loadFile(mediaFile);
    }

    @Override
    public Resource loadFile(MediaFile mediaFile) {
        if (mediaFile.getStoragePath() == null || mediaFile.getStoragePath().isEmpty()) {
            throw new FileNotFoundException("Media file has no storage path");
        }
        return mediaContentService.loadContent(mediaFile.getStoragePath());
    }

    @Override
    public Optional<MediaFile> findById(Long id) {
        return mediaMetadataService.findById(id);
    }

    @Getter
    private static class CountingInputStream extends FilterInputStream {
        private long count;

        protected CountingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int result = super.read();
            if (result != -1) {
                count++;
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int result = super.read(b, off, len);
            if (result != -1) {
                count += result;
            }
            return result;
        }
    }
}
