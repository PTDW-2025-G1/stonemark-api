package pt.estga.file.services.upload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.file.config.StorageProperties;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.enums.StorageProvider;
import pt.estga.file.exceptions.OversizeFileException;
import pt.estga.file.services.MediaMetadataService;
import pt.estga.file.services.MediaMetricsService;
import pt.estga.file.services.TempFileFactory;
import pt.estga.file.services.naming.FileNamingService;
import pt.estga.file.services.storage.FileStorageService;
import pt.estga.commonweb.exceptions.UnsupportedFileTypeException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaUploadOrchestratorTest {

    @Mock
    private MediaMetadataService mediaMetadataService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private MediaValidationService mediaValidationService;
    @Mock
    private FileNamingService fileNamingService;
    @Mock
    private MediaMetricsService metrics;
    @Mock
    private TempFileFactory tempFileFactory;

    private StorageProperties storageProperties;
    private MediaUploadOrchestrator orchestrator;

    @BeforeEach
    void setUp() throws Exception {
        storageProperties = new StorageProperties();
        storageProperties.setMaxUploadSize(10 * 1024 * 1024);
        storageProperties.setAllowedMimeTypes(List.of("image/jpeg", "image/png"));

        when(tempFileFactory.createTempFile(anyString(), anyString()))
                .thenAnswer(inv -> java.nio.file.Files.createTempFile("test-upload-", ".tmp"));

        orchestrator = new MediaUploadOrchestrator(
                mediaMetadataService,
                fileStorageService,
                mediaValidationService,
                fileNamingService,
                storageProperties,
                tempFileFactory,
                metrics
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
        when(fileNamingService.generatePath(anyString())).thenReturn(relativePath);
        when(mediaValidationService.isAllowedImage(any(), anySet())).thenReturn(true);

        MediaFile savedMedia = MediaFile.createForProcessing(storedFilename, filename, StorageProvider.LOCAL);
        savedMedia.setId(UUID.randomUUID());
        when(fileStorageService.storeFile(any(), eq(relativePath), anyLong())).thenReturn(relativePath);
        when(mediaMetadataService.saveMetadataWithRetry(any())).thenReturn(savedMedia);

        MediaFile result = orchestrator.orchestrateUpload(input, filename);

        assertNotNull(result);
        verify(metrics).recordUploadAttempt();
        verify(metrics).recordUploadSuccess(eq((long) content.length), anyLong());
        verify(fileStorageService).storeFile(any(), eq(relativePath), anyLong());
        verify(mediaMetadataService).saveMetadataWithRetry(any());
    }

    @Test
    @DisplayName("should reject file exceeding max upload size")
    void shouldRejectOversizeFile() {
        byte[] content = new byte[11 * 1024 * 1024];
        InputStream input = new ByteArrayInputStream(content);

        assertThrows(OversizeFileException.class,
                () -> orchestrator.orchestrateUpload(input, "large.jpg"));

        verify(metrics).recordUploadAttempt();
        verify(metrics).recordUploadRejected();
        verifyNoInteractions(fileStorageService);
        verifyNoInteractions(mediaValidationService);
    }

    @Test
    @DisplayName("should reject file with unsupported MIME type")
    void shouldRejectUnsupportedMimeType() throws Exception {
        byte[] content = "plain text".getBytes();
        InputStream input = new ByteArrayInputStream(content);

        when(mediaValidationService.isAllowedImage(any(), anySet())).thenReturn(false);

        assertThrows(UnsupportedFileTypeException.class,
                () -> orchestrator.orchestrateUpload(input, "test.txt"));

        verify(metrics).recordUploadAttempt();
        verify(metrics).recordUploadRejected();
        verify(fileStorageService, never()).storeFile(any(), any(), anyLong());
    }

    @Test
    @DisplayName("should throw on storage failure without persisting entity")
    void shouldHandleStorageFailure() throws Exception {
        byte[] content = new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
        InputStream input = new ByteArrayInputStream(content);
        String filename = "test.jpg";
        String storedFilename = "uuid.jpg";

        when(fileNamingService.generateStoredFilename(filename)).thenReturn(storedFilename);
        when(fileNamingService.generatePath(anyString())).thenReturn("ab/cd/uuid.jpg");
        when(mediaValidationService.isAllowedImage(any(), anySet())).thenReturn(true);
        when(fileStorageService.storeFile(any(), any(), anyLong())).thenThrow(new RuntimeException("Disk full"));

        assertThrows(RuntimeException.class,
                () -> orchestrator.orchestrateUpload(input, filename));

        verify(mediaMetadataService, never()).saveMetadataWithRetry(any());
        verify(mediaMetadataService, never()).publishAfterCommit(any());
    }
}

