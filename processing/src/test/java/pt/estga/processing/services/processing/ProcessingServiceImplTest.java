package pt.estga.processing.services.processing;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.file.api.MediaOperations;
import pt.estga.file.dtos.MediaFileDto;
import pt.estga.intake.api.IntakeSubmissionApi;
import pt.estga.intake.api.SubmissionRef;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.services.similarity.SimilarityService;
import pt.estga.processing.vision.DetectionResult;
import pt.estga.processing.vision.VisionClient;
import pt.estga.shared.core.models.EmbeddingVector;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Exercises the processing pipeline with a stubbed Vision client and a seeded
 * (mocked) evidence corpus, so the flow can be verified without a real model or database.
 */
@ExtendWith(MockitoExtension.class)
class ProcessingServiceImplTest {

    private static final UUID SUBMISSION_ID = UUID.randomUUID();

    @Mock
    private IntakeSubmissionApi intakeApi;
    @Mock
    private MarkEvidenceProcessingRepository processingRepository;
    @Mock
    private VisionClient visionClient;
    @Mock
    private MediaOperations fileStorage;
    @Mock
    private SimilarityService similarityService;
    @Mock
    private ProcessingPersistenceService persistence;

    private ProcessingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProcessingServiceImpl(
                intakeApi, processingRepository, visionClient,
                fileStorage, similarityService, new SimpleMeterRegistry(), persistence);
    }

    private void givenSubmission(UUID fileId) {
        when(intakeApi.findById(SUBMISSION_ID)).thenReturn(Optional.of(new SubmissionRef(SUBMISSION_ID, fileId, null, null)));
    }

    private MarkEvidenceProcessing processing() {
        return MarkEvidenceProcessing.builder()
                .id(UUID.randomUUID())
                .submissionId(SUBMISSION_ID)
                .status(ProcessingStatus.PROCESSING)
                .build();
    }

    @Test
    @DisplayName("defers (PENDING) when the Vision service is unavailable")
    void defersWhenVisionUnavailable() {
        MarkEvidenceProcessing processing = processing();
        givenSubmission(UUID.randomUUID());
        when(persistence.createOrReuseProcessingRecord(SUBMISSION_ID)).thenReturn(processing);
        when(visionClient.isAvailable()).thenReturn(Boolean.FALSE);

        service.processSubmission(SUBMISSION_ID);

        verify(persistence).setPending(processing.getId());
        verifyNoInteractions(similarityService);
    }

    @Test
    @DisplayName("skips when the record is already processed or in progress")
    void skipsWhenAlreadyProcessed() {
        givenSubmission(UUID.randomUUID());
        when(persistence.createOrReuseProcessingRecord(SUBMISSION_ID)).thenReturn(null);

        service.processSubmission(SUBMISSION_ID);

        verifyNoInteractions(visionClient, similarityService);
        verify(persistence, never()).finalizeSuccess(any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("fails permanently when the original media file is missing")
    void failsWhenMediaMissing() {
        UUID fileId = UUID.randomUUID();
        MarkEvidenceProcessing processing = processing();
        givenSubmission(fileId);
        when(persistence.createOrReuseProcessingRecord(SUBMISSION_ID)).thenReturn(processing);
        when(visionClient.isAvailable()).thenReturn(Boolean.TRUE);
        when(fileStorage.existsById(fileId)).thenReturn(Boolean.FALSE);

        service.processSubmission(SUBMISSION_ID);

        verify(persistence).setFailed(eq(processing.getId()), contains("not found"), eq(true), anyLong());
        verifyNoInteractions(similarityService);
    }

    @Test
    @DisplayName("fails permanently when the model returns an empty embedding")
    void failsOnEmptyEmbedding() {
        UUID fileId = UUID.randomUUID();
        MarkEvidenceProcessing processing = processing();
        givenSubmission(fileId);
        when(persistence.createOrReuseProcessingRecord(SUBMISSION_ID)).thenReturn(processing);
        when(visionClient.isAvailable()).thenReturn(Boolean.TRUE);
        when(fileStorage.existsById(fileId)).thenReturn(Boolean.TRUE);
        when(fileStorage.findById(fileId))
                .thenReturn(Optional.of(new MediaFileDto(fileId, "f.jpg", "mark.jpg", 10L, "UPLOADED", null)));
        when(fileStorage.openStream(fileId)).thenReturn(Optional.of(InputStream.nullInputStream()));
        when(visionClient.detectMark(any(), anyString())).thenReturn(new DetectionResult(false, new float[0], null, null, null));

        service.processSubmission(SUBMISSION_ID);

        verify(persistence).setFailed(eq(processing.getId()), anyString(), eq(true), anyLong());
        verifyNoInteractions(similarityService);
    }

    @Test
    @DisplayName("embeds and produces suggestions on the happy path")
    void producesSuggestions() {
        UUID fileId = UUID.randomUUID();
        MarkEvidenceProcessing processing = processing();
        List<MarkSuggestion> suggestions = List.of(
                MarkSuggestion.builder().markId(UUID.randomUUID()).confidence(0.9).build());

        givenSubmission(fileId);
        when(persistence.createOrReuseProcessingRecord(SUBMISSION_ID)).thenReturn(processing);
        when(visionClient.isAvailable()).thenReturn(Boolean.TRUE);
        when(fileStorage.existsById(fileId)).thenReturn(Boolean.TRUE);
        when(fileStorage.findById(fileId))
                .thenReturn(Optional.of(new MediaFileDto(fileId, "f.jpg", "mark.jpg", 10L, "UPLOADED", null)));
        when(fileStorage.openStream(fileId)).thenReturn(Optional.of(InputStream.nullInputStream()));
        when(visionClient.detectMark(any(), eq("mark.jpg")))
                .thenReturn(new DetectionResult(true, new float[]{1f, 0f, 0f}, null, null, null));
        when(similarityService.findSimilar(eq(processing), anyInt())).thenReturn(suggestions);

        service.processSubmission(SUBMISSION_ID);

        verify(similarityService).findSimilar(eq(processing), anyInt());
        verify(persistence).finalizeSuccess(eq(processing.getId()), any(EmbeddingVector.class), eq(suggestions), anyLong());
    }
}
