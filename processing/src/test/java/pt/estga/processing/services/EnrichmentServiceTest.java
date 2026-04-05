package pt.estga.processing.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.services.draft.DraftMarkEvidenceCommandService;
import pt.estga.processing.services.draft.DraftMarkEvidenceQueryService;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.processing.services.enrichers.Enricher;

import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EnrichmentServiceTest {

    private Enricher enricher;
    private DraftMarkEvidenceCommandService draftCommandService;
    private DraftMarkEvidenceQueryService draftQueryService;
    private MarkEvidenceSubmissionQueryService submissionQueryService;
    private List<Enricher> enrichers;
    private EnrichmentService service;
    private Clock clock;

    private final Long submissionId = 42L;
    private MarkEvidenceSubmission submission;
    private DraftMarkEvidence initialDraft;
    private DraftMarkEvidence lockedDraft;
    private AtomicReference<DraftMarkEvidence> dbDraft;

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

    private void invokeInternal(EnrichmentService svc, Long submissionId) {
        try {
            java.lang.reflect.Method m = EnrichmentService.class.getDeclaredMethod("enrichSubmissionInternal", Long.class);
            m.setAccessible(true);
            m.invoke(svc, submissionId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setStaleTimeout(EnrichmentService svc, long minutes) {
        try {
            java.lang.reflect.Field f = EnrichmentService.class.getDeclaredField("staleTimeoutMinutes");
            f.setAccessible(true);
            f.setLong(svc, minutes);
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

        clock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);

        service = new EnrichmentService(
                enrichers,
                draftCommandService,
                draftQueryService,
                submissionQueryService,
                mock(PlatformTransactionManager.class),
                clock
        );
        setStaleTimeout(service, 1L);

        submission = MarkEvidenceSubmission.builder().id(submissionId).build();

        initialDraft = DraftMarkEvidence.builder()
                .id(100L)
                .active(true)
                .processingStatus(ProcessingStatus.PENDING)
                .build();

        lockedDraft = DraftMarkEvidence.builder()
                .id(initialDraft.getId())
                .active(true)
                .processingStatus(ProcessingStatus.PENDING)
                .build();

        dbDraft = new AtomicReference<>(copyOf(lockedDraft));

        when(submissionQueryService.findById(submissionId)).thenReturn(Optional.of(submission));
        when(draftQueryService.findBySubmissionId(submissionId)).thenReturn(Optional.of(initialDraft));
        when(draftQueryService.findByIdForUpdate(initialDraft.getId())).thenAnswer(invocation -> Optional.of(copyOf(dbDraft.get())));

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
        setDbEmbedding(new float[]{0.1f});
        invokeInternal(service, submissionId);

        DraftMarkEvidence finalDraft = dbDraft.get();
        assertEquals(ProcessingStatus.COMPLETED, finalDraft.getProcessingStatus());
        assertNull(finalDraft.getProcessingError());
    }

    @Test
    public void enrichSubmission_shouldMarkFailed_whenEnricherThrows() {
        doThrow(new RuntimeException("boom")).when(enricher).enrich(any(Long.class));
        invokeInternal(service, submissionId);

        DraftMarkEvidence finalDraft = dbDraft.get();
        assertEquals(ProcessingStatus.FAILED, finalDraft.getProcessingStatus());
        assertNotNull(finalDraft.getProcessingError());
        assertTrue(finalDraft.getProcessingError().contains("boom"));
    }

    @Test
    public void enrichSubmission_shouldSkip_whenAlreadyCompleted() {
        initialDraft.setProcessingStatus(ProcessingStatus.COMPLETED);
        invokeInternal(service, submissionId);

        verify(enricher, times(0)).enrich(any(Long.class));
        verify(draftCommandService, times(0)).update(any());
    }

    @Test
    public void enrichSubmission_shouldFail_whenEmbeddingMissing() {
        setDbEmbedding(null);
        invokeInternal(service, submissionId);

        DraftMarkEvidence finalDraft = dbDraft.get();
        assertEquals(ProcessingStatus.FAILED, finalDraft.getProcessingStatus());
        assertNotNull(finalDraft.getProcessingError());
        assertTrue(finalDraft.getProcessingError().contains("Missing embedding"));
    }

    @Test
    public void enrichSubmission_shouldSkip_whenDraftInactive() {
        initialDraft.setActive(false);
        invokeInternal(service, submissionId);

        verify(enricher, times(0)).enrich(any());
        verify(draftCommandService, times(0)).update(any());
    }

    @Test
    public void enrichSubmission_shouldSkip_whenAlreadyInProgress() {
        initialDraft.setProcessingStatus(ProcessingStatus.IN_PROGRESS);
        setLastModifiedAt(initialDraft, Instant.now(clock));
        invokeInternal(service, submissionId);

        verify(enricher, times(0)).enrich(any());
    }

    @Test
    public void enrichSubmission_shouldRecover_whenInProgressIsStale() {
        initialDraft.setProcessingStatus(ProcessingStatus.IN_PROGRESS);
        setLastModifiedAt(initialDraft, Instant.now(clock).minus(Duration.ofMinutes(20)));
        setDbEmbedding(new float[]{0.1f});

        invokeInternal(service, submissionId);

        DraftMarkEvidence finalDraft = dbDraft.get();
        assertEquals(ProcessingStatus.COMPLETED, finalDraft.getProcessingStatus());
        assertNull(finalDraft.getProcessingError());
    }

    @Test
    public void enrichSubmission_shouldComplete_whenOneEnricherFails_butOtherSucceeds() {
        Enricher failing = mock(Enricher.class);
        Enricher working = mock(Enricher.class);

        doThrow(new RuntimeException("fail")).when(failing).enrich(any());
        doAnswer(invocation -> {
            setDbEmbedding(new float[]{0.1f});
            return null;
        }).when(working).enrich(any());

        service = new EnrichmentService(List.of(failing, working), draftCommandService, draftQueryService, submissionQueryService, mock(PlatformTransactionManager.class), clock);
        setStaleTimeout(service, 1L);
        invokeInternal(service, submissionId);

        DraftMarkEvidence finalDraft = dbDraft.get();
        assertEquals(ProcessingStatus.COMPLETED, finalDraft.getProcessingStatus());
        assertNull(finalDraft.getProcessingError());
    }

    @Test
    public void enrichSubmission_shouldMergeErrors_whenMultipleEnrichersFail() {
        Enricher e1 = mock(Enricher.class);
        Enricher e2 = mock(Enricher.class);

        doThrow(new RuntimeException("fail1")).when(e1).enrich(any());
        doThrow(new RuntimeException("fail2")).when(e2).enrich(any());

        service = new EnrichmentService(List.of(e1, e2), draftCommandService, draftQueryService, submissionQueryService, mock(PlatformTransactionManager.class), clock);
        setStaleTimeout(service, 1L);
        setDbEmbedding(null); // no embedding to force FAILED
        invokeInternal(service, submissionId);

        DraftMarkEvidence finalDraft = dbDraft.get();
        assertEquals(ProcessingStatus.FAILED, finalDraft.getProcessingStatus());
        assertTrue(finalDraft.getProcessingError().contains("fail1"));
        assertTrue(finalDraft.getProcessingError().contains("fail2"));
    }
}
