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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        // This instance is returned for findByIdForUpdate so modifications persist across calls in the test.
        lockedDraft = DraftMarkEvidence.builder()
                .id(initialDraft.getId())
                .active(true)
                .processingStatus(ProcessingStatus.PENDING)
                .build();

        when(submissionQueryService.findById(submissionId)).thenReturn(Optional.of(submission));
        when(draftQueryService.findBySubmissionId(submissionId)).thenReturn(Optional.of(initialDraft));
        when(draftQueryService.findByIdForUpdate(initialDraft.getId())).thenAnswer(invocation -> Optional.of(lockedDraft));

        // Make update return its argument to mimic repository behavior
        when(draftCommandService.update(any(DraftMarkEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void enrichSubmission_shouldMarkCompleted_whenAllEnrichersSucceed() {
        // Arrange - ensure embedding exists so validation passes
        lockedDraft.setEmbedding(new float[]{0.1f});

        // Act
        service.enrichSubmission(submissionId);

        // Assert
        ArgumentCaptor<DraftMarkEvidence> captor = ArgumentCaptor.forClass(DraftMarkEvidence.class);
        // Expect two updates: set IN_PROGRESS and final update
        verify(draftCommandService, times(2)).update(captor.capture());

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

        // Assert
        ArgumentCaptor<DraftMarkEvidence> captor = ArgumentCaptor.forClass(DraftMarkEvidence.class);
        // Expect three updates: IN_PROGRESS, per-enricher failure update, and final update
        verify(draftCommandService, times(3)).update(captor.capture());

        List<DraftMarkEvidence> updated = captor.getAllValues();
        DraftMarkEvidence finalDraft = updated.get(updated.size() - 1);

        assertEquals(ProcessingStatus.FAILED, finalDraft.getProcessingStatus(), "Final draft should be marked FAILED when enricher throws");
        assertNotNull(finalDraft.getProcessingError(), "Processing error should be set when enricher throws");
    }
}
