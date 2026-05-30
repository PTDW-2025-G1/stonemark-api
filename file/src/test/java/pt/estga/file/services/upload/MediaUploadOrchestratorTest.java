package pt.estga.file.services.upload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.dtos.SaveResult;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.MediaStatus;
import pt.estga.file.exceptions.MediaPersistenceException;
import pt.estga.file.exceptions.OversizeFileException;
import pt.estga.file.services.MediaContentService;
import pt.estga.file.services.MediaMetadataService;
import pt.estga.file.services.MediaMetricsService;
import pt.estga.file.services.naming.FileNamingService;
import pt.estga.file.services.naming.StoragePathStrategy;
import pt.estga.sharedweb.exceptions.UnsupportedFileTypeException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaUploadOrchestratorTest {

    @Mock
    private MediaMetadataService mediaMetadataService;
    @Mock
    private MediaContentService mediaContentService;
    @Mock
    private MediaValidationService mediaValidationService;
    @Mock
    private FileNamingService fileNamingService;
    @Mock
    private StoragePathStrategy storagePathStrategy;
    @Mock
    private MediaMetricsService metrics;

    private StorageProperties storageProperties;
    private MediaUploadOrchestrator orchestrator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setMaxUploadSize(10 * 1024 * 1024); // 10MB
        storageProperties.setAllowedMimeTypes(List.of("image/jpeg", "image/png"));

        ObjectProvider<MediaMetricsService> metricsProvider = mock(ObjectProvider.class);
        when(metricsProvider.getIfAvailable()).thenReturn(metrics);

        orchestrator = new MediaUploadOrchestrator(
                mediaMetadataService,
                mediaContentService,
                mediaValidationService,
                fileNamingService,
                storageProperties,
                storagePathStrategy,
                metricsProvider
        );
    }

    @Test
    @DisplayName("should successfully upload and process a valid file")
    void shouldUploadSuccessfully() throws Exception {
        byte[] content = new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00 };
        InputStream input = new ByteArrayInputStream(content);
        String filename = "test.jpg";
        String storedFilename = UUID.randomUUID() + ".jpg";
        String relativePath = "ab/cd/" + storedFilename;

        when(fileNamingService.generateStoredFilename(filename)).thenReturn(storedFilename);
        when(storagePathStrategy.generatePath(any())).thenReturn(relativePath);
        when(mediaValidationService.isAllowedImage(any(), anySet())).thenReturn(true);

        MediaFile savedMedia = MediaFile.createForProcessing(storedFilename, filename, StoragePropertiesConfig.provider());
        savedMedia.setId(UUID.randomUUID());
        when(mediaMetadataService.saveMetadata(any())).thenReturn(savedMedia);
        when(mediaContentService.saveContent(any(), eq(relativePath))).thenReturn(new SaveResult(relativePath, content.length));
        when(mediaMetadataService.saveMetadataWithRetriesAndPublish(any(), any())).thenReturn(savedMedia);

        MediaFile result = orchestrator.orchestrateUpload(input, filename, content.length);

        assertNotNull(result);
        verify(metrics).recordUploadAttempt();
        verify(metrics).recordUploadSuccess(eq((long) content.length), anyLong());
        verify(mediaContentService).saveContent(any(), eq(relativePath));
        verify(mediaMetadataService).saveMetadataWithRetriesAndPublish(any(), any());
    }

    @Test
    @DisplayName("should reject file exceeding max upload size")
    void shouldRejectOversizeFile() {
        byte[] content = new byte[11 * 1024 * 1024]; // 11MB
        InputStream input = new ByteArrayInputStream(content);

        assertThrows(OversizeFileException.class,
                () -> orchestrator.orchestrateUpload(input, "large.jpg", content.length));

        verify(metrics).recordUploadAttempt();
        verify(metrics).recordUploadRejected();
        verifyNoInteractions(mediaContentService);
        verifyNoInteractions(mediaValidationService);
    }

    @Test
    @DisplayName("should reject file with unsupported MIME type")
    void shouldRejectUnsupportedMimeType() throws Exception {
        byte[] content = "plain text".getBytes();
        InputStream input = new ByteArrayInputStream(content);

        when(mediaValidationService.isAllowedImage(any(), anySet())).thenReturn(false);

        assertThrows(UnsupportedFileTypeException.class,
                () -> orchestrator.orchestrateUpload(input, "test.txt", content.length));

        verify(metrics).recordUploadAttempt();
        verify(metrics).recordUploadRejected();
        verify(mediaContentService, never()).saveContent(any(), any());
    }

    @Test
    @DisplayName("should handle storage failure and mark as failed")
    void shouldHandleStorageFailure() throws Exception {
        byte[] content = new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
        InputStream input = new ByteArrayInputStream(content);
        String filename = "test.jpg";
        String storedFilename = "uuid.jpg";

        when(fileNamingService.generateStoredFilename(filename)).thenReturn(storedFilename);
        when(storagePathStrategy.generatePath(any())).thenReturn("ab/cd/uuid.jpg");
        when(mediaValidationService.isAllowedImage(any(), anySet())).thenReturn(true);

        MediaFile savedMedia = MediaFile.createForProcessing(storedFilename, filename, StoragePropertiesConfig.provider());
        savedMedia.setId(UUID.randomUUID());
        when(mediaMetadataService.saveMetadata(any())).thenReturn(savedMedia);
        when(mediaContentService.saveContent(any(), any())).thenThrow(new RuntimeException("Disk full"));

        assertThrows(RuntimeException.class,
                () -> orchestrator.orchestrateUpload(input, filename));

        verify(mediaMetadataService, atLeastOnce()).saveMetadata(argThat(m ->
                m.getStatus() == MediaStatus.FAILED));
    }

    private static class StoragePropertiesConfig {
            static pt.estga.file.enums.StorageProvider provider() {
                return pt.estga.file.enums.StorageProvider.LOCAL;
            }
        }
}

