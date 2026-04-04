package pt.estga.processing.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.services.draft.DraftMarkEvidenceCommandService;
import pt.estga.processing.services.draft.DraftMarkEvidenceQueryService;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.processing.services.enrichers.Enricher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class EnrichmentServiceTest {

    private Enricher enricher;
    private DraftMarkEvidenceCommandService draftCommandService;
    private DraftMarkEvidenceQueryService draftQueryService;
    private MarkEvidenceSubmissionQueryService submissionQueryService;
    private List<Enricher> enrichers;
    private EnrichmentService service;

    private final Long submissionId = 42L;
    private MarkEvidenceSubmission submission;
    private DraftMarkEvidence initialDraft;
    private DraftMarkEvidence lockedDraft;
    private AtomicReference<DraftMarkEvidence> dbDraft;

    // Reflection helper to set the protected lastModifiedAt field defined on AuditedEntity
    private static void setLastModifiedAt(Object target, Instant instant) {
        try {
            Class<?> c = target.getClass();
            java.lang.reflect.Field field = null;
            while (c != null) {
                try {
                    field = c.getDeclaredField("lastModifiedAt");
                    break;
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            if (field == null) throw new IllegalStateException("Field lastModifiedAt not found on target");
            field.setAccessible(true);
            field.set(target, instant);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void setUp() {
        enricher = mock(Enricher.class);
        draftCommandService = mock(DraftMarkEvidenceCommandService.class);
        draftQueryService = mock(DraftMarkEvidenceQueryService.class);
        submissionQueryService = mock(MarkEvidenceSubmissionQueryService.class);

        enrichers = Collections.singletonList(enricher);

        service = new EnrichmentService(
                enrichers,
                draftCommandService,
                draftQueryService,
                submissionQueryService
        );

        submission = MarkEvidenceSubmission.builder().id(submissionId).build();

        initialDraft = DraftMarkEvidence.builder()
                .id(100L)
                .active(true)
                .processingStatus(ProcessingStatus.PENDING)
                .build();

        // This instance is used to seed the DB-like storage returned for findByIdForUpdate
        lockedDraft = DraftMarkEvidence.builder()
                .id(initialDraft.getId())
                .active(true)
                .processingStatus(ProcessingStatus.PENDING)
                .build();
        // Initialize in-memory DB draft and wire mocks to use it. findByIdForUpdate returns a copy to simulate separate transactional instances.
        dbDraft = new AtomicReference<>(copyOf(lockedDraft));

        when(submissionQueryService.findById(submissionId)).thenReturn(Optional.of(submission));
        when(draftQueryService.findBySubmissionId(submissionId)).thenReturn(Optional.of(initialDraft));
        when(draftQueryService.findByIdForUpdate(initialDraft.getId())).thenAnswer(invocation -> Optional.of(copyOf(dbDraft.get())));

        // Make update persist into the in-memory DB and return the passed argument to mimic repository behavior
        when(draftCommandService.update(any(DraftMarkEvidence.class))).thenAnswer(invocation -> {
            DraftMarkEvidence arg = invocation.getArgument(0);
            dbDraft.set(copyOf(arg));
            return arg;
        });
    }

    private DraftMarkEvidence copyOf(DraftMarkEvidence source) {
        if (source == null) return null;
        return DraftMarkEvidence.builder()
                .id(source.getId())
                .active(source.getActive())
                .processingStatus(source.getProcessingStatus())
                .embedding(source.getEmbedding() == null ? null : source.getEmbedding().clone())
                .processingError(source.getProcessingError())
                .build();
    }

    private void setDbEmbedding(float[] embedding) {
        DraftMarkEvidence cur = dbDraft.get();
        DraftMarkEvidence temp = copyOf(cur);
        temp.setEmbedding(embedding == null ? null : embedding.clone());
        dbDraft.set(temp);
    }

    @Test
    public void enrichSubmission_shouldMarkCompleted_whenAllEnrichersSucceed() {
        // Arrange - ensure embedding exists so validation passes
        lockedDraft.setEmbedding(new float[]{0.1f});
        setDbEmbedding(lockedDraft.getEmbedding());

        // Act
        service.enrichSubmission(submissionId);

        // Assert final state only; avoid brittle exact update counts
        ArgumentCaptor<DraftMarkEvidence> captor = ArgumentCaptor.forClass(DraftMarkEvidence.class);
        verify(draftCommandService, atLeast(1)).update(captor.capture());

        List<DraftMarkEvidence> updated = captor.getAllValues();
        DraftMarkEvidence finalDraft = updated.get(updated.size() - 1);

        assertEquals(ProcessingStatus.COMPLETED, finalDraft.getProcessingStatus(), "Final draft should be marked COMPLETED");
        assertNull(finalDraft.getProcessingError(), "Processing error should be null when completed successfully");
    }

    @Test
    public void enrichSubmission_shouldMarkFailed_whenEnricherThrows() {
        // Arrange - make the enricher throw
        RuntimeException boom = new RuntimeException("boom");
        org.mockito.Mockito.doThrow(boom).when(enricher).enrich(any(Long.class));

        // Act
        service.enrichSubmission(submissionId);

        // Assert final state only; avoid brittle exact update counts
        ArgumentCaptor<DraftMarkEvidence> captor = ArgumentCaptor.forClass(DraftMarkEvidence.class);
        verify(draftCommandService, atLeast(1)).update(captor.capture());

        List<DraftMarkEvidence> updated = captor.getAllValues();
        DraftMarkEvidence finalDraft = updated.get(updated.size() - 1);

        assertEquals(ProcessingStatus.FAILED, finalDraft.getProcessingStatus(), "Final draft should be marked FAILED when enricher throws");
        assertNotNull(finalDraft.getProcessingError(), "Processing error should be set when enricher throws");
    }

    @Test
    public void enrichSubmission_shouldSkip_whenAlreadyCompleted() {
        // Arrange - mark the draft as already completed
        initialDraft.setProcessingStatus(ProcessingStatus.COMPLETED);

        // Act
        service.enrichSubmission(submissionId);

        // Assert - no enrichers or updates should be invoked
        verify(enricher, times(0)).enrich(any(Long.class));
        verify(draftCommandService, times(0)).update(any(DraftMarkEvidence.class));
    }

    @Test
    public void enrichSubmission_shouldFail_whenEmbeddingMissing() {
        // Arrange - ensure embedding is missing on locked draft
        lockedDraft.setEmbedding(null);
        setDbEmbedding(null);

        // Act
        service.enrichSubmission(submissionId);

        // Assert - final draft should be FAILED and contain processingError
        ArgumentCaptor<DraftMarkEvidence> captor = ArgumentCaptor.forClass(DraftMarkEvidence.class);
        verify(draftCommandService, atLeast(1)).update(captor.capture());

        List<DraftMarkEvidence> updated = captor.getAllValues();
        DraftMarkEvidence finalDraft = updated.get(updated.size() - 1);

        assertEquals(ProcessingStatus.FAILED, finalDraft.getProcessingStatus(), "Final draft should be marked FAILED when embedding is missing");
        assertNotNull(finalDraft.getProcessingError(), "Processing error should be set when embedding validation fails");
    }

    @Test
    public void enrichSubmission_shouldSkip_whenDraftInactive() {
        // Arrange - mark the draft as inactive
        initialDraft.setActive(false);

        // Act
        service.enrichSubmission(submissionId);

        // Assert - enricher and updates must not be called
        verify(enricher, times(0)).enrich(any(Long.class));
        verify(draftCommandService, times(0)).update(any(DraftMarkEvidence.class));
    }

    @Test
    public void enrichSubmission_shouldSkip_whenAlreadyInProgress() {
        // Arrange - draft is already in progress and not stale
        initialDraft.setProcessingStatus(ProcessingStatus.IN_PROGRESS);
        setLastModifiedAt(initialDraft, Instant.now());

        // Act
        service.enrichSubmission(submissionId);

        // Assert - should skip without calling enrichers
        verify(enricher, times(0)).enrich(any(Long.class));
    }

    @Test
    public void enrichSubmission_shouldRecover_whenInProgressIsStale() {
        // Arrange - simulate stale IN_PROGRESS older than 10 minutes
        initialDraft.setProcessingStatus(ProcessingStatus.IN_PROGRESS);
        setLastModifiedAt(initialDraft, Instant.now().minus(Duration.ofMinutes(20)));

        // Ensure final locked draft contains embedding so recovery can complete
        lockedDraft.setEmbedding(new float[]{0.1f});
        setDbEmbedding(lockedDraft.getEmbedding());

        // Act
        service.enrichSubmission(submissionId);

        // Assert final state only; avoid brittle exact update counts
        ArgumentCaptor<DraftMarkEvidence> captor = ArgumentCaptor.forClass(DraftMarkEvidence.class);
        verify(draftCommandService, atLeast(1)).update(captor.capture());

        List<DraftMarkEvidence> updated = captor.getAllValues();
        DraftMarkEvidence finalDraft = updated.get(updated.size() - 1);

        assertEquals(ProcessingStatus.COMPLETED, finalDraft.getProcessingStatus(), "Final draft should be COMPLETED after recovering from stale IN_PROGRESS");
    }

    @Test
    public void enrichSubmission_shouldFail_whenEnricherDoesNotProduceEmbedding() {
        // Arrange - enricher runs but does not produce embedding (lockedDraft remains null embedding)
        lockedDraft.setEmbedding(null);
        setDbEmbedding(null);

        // Act
        service.enrichSubmission(submissionId);

        // Assert
        verify(enricher, atLeast(1)).enrich(any(Long.class));
        ArgumentCaptor<DraftMarkEvidence> captor = ArgumentCaptor.forClass(DraftMarkEvidence.class);
        verify(draftCommandService, atLeast(1)).update(captor.capture());

        List<DraftMarkEvidence> updated = captor.getAllValues();
        DraftMarkEvidence finalDraft = updated.get(updated.size() - 1);

        assertEquals(ProcessingStatus.FAILED, finalDraft.getProcessingStatus());
        assertNotNull(finalDraft.getProcessingError());
    }

    @Test
    public void enrichSubmission_shouldComplete_whenOneEnricherFails_butOthersSucceed() {
        // Arrange - two enrichers: one fails, one succeeds and writes embedding to shared lockedDraft
        Enricher failing = mock(Enricher.class);
        Enricher working = mock(Enricher.class);

        org.mockito.Mockito.doThrow(new RuntimeException("fail")).when(failing).enrich(any(Long.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            // Simulate successful enricher persisting embedding to the DB representation
            setDbEmbedding(new float[]{0.1f});
            return null;
        }).when(working).enrich(any(Long.class));

        service = new EnrichmentService(List.of(failing, working), draftCommandService, draftQueryService, submissionQueryService);

        // Act
        service.enrichSubmission(submissionId);

        // Assert
        ArgumentCaptor<DraftMarkEvidence> captor = ArgumentCaptor.forClass(DraftMarkEvidence.class);
        verify(draftCommandService, atLeast(1)).update(captor.capture());

        List<DraftMarkEvidence> updated = captor.getAllValues();
        DraftMarkEvidence finalDraft = updated.get(updated.size() - 1);

        assertEquals(ProcessingStatus.COMPLETED, finalDraft.getProcessingStatus());
    }
}
