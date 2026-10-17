package pt.estga.review.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.mark.api.MarkQueryService;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;

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

    private static final Long SUBMISSION_ID = 1L;
    private static final Long MARK_ID = 5L;
    private static final Long OCCURRENCE_ID = 9L;
    private static final UUID FILE_ID = UUID.randomUUID();

    @Mock
    private MarkEvidenceSubmissionRepository submissionRepository;
    @Mock
    private MarkEvidenceProcessingRepository processingRepository;
    @Mock
    private MarkSuggestionRepository suggestionRepository;
    @Mock
    private MarkEvidenceReviewRepository markEvidenceReviewRepository;
    @Mock
    private MarkQueryService markQueryService;
    @Mock
    private ReviewExecutor executor;

    @InjectMocks
    private ReviewService reviewService;

    private MarkEvidenceSubmission receivedSubmission() {
        return MarkEvidenceSubmission.builder()
                .id(SUBMISSION_ID)
                .originalMediaFileId(FILE_ID)
                .status(SubmissionStatus.RECEIVED)
                .build();
    }

    @Test
    @DisplayName("approves and links evidence even when the submission was never processed")
    void approvesWithoutProcessing() {
        MarkEvidenceSubmission submission = receivedSubmission();
        MarkDto mark = new MarkDto(MARK_ID, "Cross", null, null, null, Boolean.TRUE);
        MarkEvidenceReview label = MarkEvidenceReview.builder()
                .id(100L).submissionId(SUBMISSION_ID)
                .decision(ReviewDecision.APPROVED).selectedMarkId(MARK_ID).build();

        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));
        when(processingRepository.findOverviewBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(markEvidenceReviewRepository.existsBySubmissionId(SUBMISSION_ID)).thenReturn(Boolean.FALSE);
        when(markQueryService.findMarkById(MARK_ID)).thenReturn(Optional.of(mark));
        when(markQueryService.findOrCreateOccurrence(MARK_ID, true))
                .thenReturn(new MarkOccurrenceDto(OCCURRENCE_ID, MARK_ID, null, mark, null, Boolean.TRUE));
        when(processingRepository.findEmbeddingTextBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(executor.execute(any(), eq(ReviewDecision.APPROVED), any(), any(), any())).thenReturn(label);

        MarkEvidenceReview result = reviewService.acceptSuggestion(SUBMISSION_ID, MARK_ID, "ok");

        assertThat(result).isSameAs(label);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.PROCESSED);
        verify(markQueryService).linkFileToOccurrence(eq(FILE_ID), eq(OCCURRENCE_ID), isNull());
        verify(submissionRepository).save(submission);
    }

    @Test
    @DisplayName("rejects without processing and records the decision as a label")
    void rejectsWithoutProcessing() {
        MarkEvidenceSubmission submission = receivedSubmission();
        MarkEvidenceReview label = MarkEvidenceReview.builder()
                .id(101L).submissionId(SUBMISSION_ID)
                .decision(ReviewDecision.REJECTED).build();

        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));
        when(processingRepository.findOverviewBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(markEvidenceReviewRepository.existsBySubmissionId(SUBMISSION_ID)).thenReturn(Boolean.FALSE);
        when(executor.execute(any(), eq(ReviewDecision.REJECTED), any(), any(), any())).thenReturn(label);

        MarkEvidenceReview result = reviewService.rejectAll(SUBMISSION_ID, "blurry");

        assertThat(result).isSameAs(label);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.REJECTED);
        verifyNoInteractions(markQueryService);
    }
}
