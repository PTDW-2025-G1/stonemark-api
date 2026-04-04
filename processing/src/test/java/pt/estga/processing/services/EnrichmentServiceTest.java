package pt.estga.processing.services;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EnrichmentServiceTest {

    @Test
    public void enrichSubmission_shouldMarkCompleted_whenAllEnrichersSucceed() {
        // Arrange
        Enricher enricher = mock(Enricher.class);
        DraftMarkEvidenceCommandService draftCommandService = mock(DraftMarkEvidenceCommandService.class);
        DraftMarkEvidenceQueryService draftQueryService = mock(DraftMarkEvidenceQueryService.class);
        MarkEvidenceSubmissionQueryService submissionQueryService = mock(MarkEvidenceSubmissionQueryService.class);

        List<Enricher> enrichers = Collections.singletonList(enricher);

        EnrichmentService service = new EnrichmentService(
                enrichers,
                draftCommandService,
                draftQueryService,
                submissionQueryService
        );

        Long submissionId = 42L;
        MarkEvidenceSubmission submission = MarkEvidenceSubmission.builder().id(submissionId).build();

        DraftMarkEvidence initialDraft = DraftMarkEvidence.builder()
                .id(100L)
                .active(true)
                .processingStatus(ProcessingStatus.PENDING)
                .build();

        // The final draft returned under pessimistic lock must contain an embedding
        // so the service considers processing successful.
        DraftMarkEvidence lockedDraft = DraftMarkEvidence.builder()
                .id(initialDraft.getId())
                .active(true)
                .processingStatus(ProcessingStatus.PENDING)
                .embedding(new float[]{0.1f})
                .build();

        when(submissionQueryService.findById(submissionId)).thenReturn(Optional.of(submission));
        when(draftQueryService.findBySubmissionId(submissionId)).thenReturn(Optional.of(initialDraft));
        when(draftQueryService.findByIdForUpdate(initialDraft.getId())).thenReturn(Optional.of(lockedDraft));

        // Make update return its argument to mimic repository behavior
        when(draftCommandService.update(any(DraftMarkEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.enrichSubmission(submissionId);

        // Assert
        ArgumentCaptor<DraftMarkEvidence> captor = ArgumentCaptor.forClass(DraftMarkEvidence.class);
        // Expect at least two updates: one when marking IN_PROGRESS and one final update
        verify(draftCommandService, times(2)).update(captor.capture());

        List<DraftMarkEvidence> updated = captor.getAllValues();
        DraftMarkEvidence finalDraft = updated.get(updated.size() - 1);

        assertEquals(ProcessingStatus.COMPLETED, finalDraft.getProcessingStatus(), "Final draft should be marked COMPLETED");
        assertNull(finalDraft.getProcessingError(), "Processing error should be null when completed successfully");
    }
}
