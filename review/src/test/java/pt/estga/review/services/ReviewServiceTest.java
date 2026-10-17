package pt.estga.review.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.intake.api.IntakeSubmissionApi;
import pt.estga.intake.api.SubmissionRef;
import pt.estga.mark.api.MarkQueryService;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.processing.api.ProcessingReviewApi;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
import pt.estga.shared.core.enums.PublicationStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final UUID SUBMISSION_ID = UUID.randomUUID();
    private static final UUID MARK_ID = UUID.randomUUID();
    private static final UUID OCCURRENCE_ID = UUID.randomUUID();
    private static final UUID FILE_ID = UUID.randomUUID();

    @Mock
    private IntakeSubmissionApi intakeApi;
    @Mock
    private ProcessingReviewApi processingApi;
    @Mock
    private MarkEvidenceReviewRepository markEvidenceReviewRepository;
    @Mock
    private MarkQueryService markQueryService;
    @Mock
    private MarkCreationSuggestionService markCreationSuggestionService;
    @Mock
    private ReviewExecutor executor;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("approves and links evidence even when the submission was never processed")
    void approvesWithoutProcessing() {
        MarkDto mark = new MarkDto(MARK_ID, "Cross", null, null, PublicationStatus.DRAFT);
        MarkEvidenceReview label = MarkEvidenceReview.builder()
                .id(UUID.randomUUID()).submissionId(SUBMISSION_ID)
                .decision(ReviewDecision.APPROVED).selectedMarkId(MARK_ID).build();

        when(intakeApi.findById(SUBMISSION_ID))
                .thenReturn(Optional.of(new SubmissionRef(SUBMISSION_ID, FILE_ID, null, null)));
        when(processingApi.findOverview(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(markEvidenceReviewRepository.existsBySubmissionId(SUBMISSION_ID)).thenReturn(Boolean.FALSE);
        when(markQueryService.findMarkById(MARK_ID)).thenReturn(Optional.of(mark));
        when(markQueryService.findOrCreateOccurrence(eq(MARK_ID), isNull(), isNull(), isNull()))
                .thenReturn(new MarkOccurrenceDto(OCCURRENCE_ID, MARK_ID, null, null, null, null,
                        PublicationStatus.PUBLISHED, null, mark));
        when(processingApi.findEmbedding(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(executor.execute(eq(SUBMISSION_ID), eq(ReviewDecision.APPROVED), any(), any())).thenReturn(label);

        MarkEvidenceReview result = reviewService.acceptSuggestion(SUBMISSION_ID, MARK_ID, "ok");

        assertThat(result).isSameAs(label);
        verify(intakeApi).markProcessed(SUBMISSION_ID);
        verify(markQueryService).linkFileToOccurrence(eq(FILE_ID), eq(OCCURRENCE_ID), eq(SUBMISSION_ID), isNull());
    }

    @Test
    @DisplayName("rejects without processing and records the decision as a label")
    void rejectsWithoutProcessing() {
        MarkEvidenceReview label = MarkEvidenceReview.builder()
                .id(UUID.randomUUID()).submissionId(SUBMISSION_ID)
                .decision(ReviewDecision.REJECTED).build();

        when(intakeApi.findById(SUBMISSION_ID))
                .thenReturn(Optional.of(new SubmissionRef(SUBMISSION_ID, FILE_ID, null, null)));
        when(processingApi.findOverview(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(markEvidenceReviewRepository.existsBySubmissionId(SUBMISSION_ID)).thenReturn(Boolean.FALSE);
        when(executor.execute(eq(SUBMISSION_ID), eq(ReviewDecision.REJECTED), any(), any())).thenReturn(label);

        MarkEvidenceReview result = reviewService.rejectAll(SUBMISSION_ID, "blurry");

        assertThat(result).isSameAs(label);
        verify(intakeApi).markRejected(SUBMISSION_ID);
        verifyNoInteractions(markQueryService);
    }

    @Test
    @DisplayName("no-match keeps the submission as a candidate without provisioning a mark")
    void noMatchDoesNotProvisionMark() {
        MarkEvidenceReview label = MarkEvidenceReview.builder()
                .id(UUID.randomUUID()).submissionId(SUBMISSION_ID)
                .decision(ReviewDecision.NO_MATCH).build();

        when(intakeApi.findById(SUBMISSION_ID))
                .thenReturn(Optional.of(new SubmissionRef(SUBMISSION_ID, FILE_ID, null, null)));
        when(processingApi.findOverview(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(markEvidenceReviewRepository.existsBySubmissionId(SUBMISSION_ID)).thenReturn(Boolean.FALSE);
        when(executor.execute(eq(SUBMISSION_ID), eq(ReviewDecision.NO_MATCH), any(), any())).thenReturn(label);

        MarkEvidenceReview result = reviewService.noMatch(SUBMISSION_ID, "looks new");

        assertThat(result).isSameAs(label);
        verify(intakeApi).markProcessed(SUBMISSION_ID);
        verify(markCreationSuggestionService).recompute();
        verifyNoInteractions(markQueryService);
    }
}
