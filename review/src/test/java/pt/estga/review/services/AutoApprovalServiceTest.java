package pt.estga.review.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.estga.processing.api.ProcessingModerationView;
import pt.estga.processing.api.ProcessingReviewApi;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoApprovalServiceTest {

    private static final UUID PROCESSING_ID = UUID.randomUUID();
    private static final UUID SUBMISSION_ID = UUID.randomUUID();
    private static final UUID MARK_ID = UUID.randomUUID();
    private static final UUID OTHER_MARK_ID = UUID.randomUUID();

    @Mock
    private ProcessingReviewApi processingApi;
    @Mock
    private MarkEvidenceReviewRepository reviewRepository;
    @Mock
    private ReviewService reviewService;

    private AutoApprovalService service;

    @BeforeEach
    void setUp() {
        service = new AutoApprovalService(processingApi, reviewRepository, reviewService);
        ReflectionTestUtils.setField(service, "minConfidence", 0.9);
        ReflectionTestUtils.setField(service, "minMargin", 0.1);
    }

    @Test
    @DisplayName("approves a confident, unambiguous match without a human")
    void approvesConfidentMatch() {
        givenCandidate(ProcessingStatus.COMPLETED, 0.95);
        when(reviewRepository.existsBySubmissionId(SUBMISSION_ID)).thenReturn(Boolean.FALSE);
        when(processingApi.findSuggestions(PROCESSING_ID)).thenReturn(List.of(suggestion(MARK_ID, 0.95)));

        assertThat(service.approveEligible()).isEqualTo(1);

        verify(reviewService).autoApprove(SUBMISSION_ID, MARK_ID);
    }

    @Test
    @DisplayName("does not approve when the best match is not clearly ahead")
    void skipsAmbiguousMatch() {
        givenCandidate(ProcessingStatus.COMPLETED, 0.95);
        when(reviewRepository.existsBySubmissionId(SUBMISSION_ID)).thenReturn(Boolean.FALSE);
        when(processingApi.findSuggestions(PROCESSING_ID))
                .thenReturn(List.of(suggestion(MARK_ID, 0.95), suggestion(OTHER_MARK_ID, 0.9)));

        assertThat(service.approveEligible()).isZero();

        verify(reviewService, never()).autoApprove(any(), any());
    }

    @Test
    @DisplayName("does not approve a submission that was already reviewed")
    void skipsAlreadyReviewed() {
        givenCandidate(ProcessingStatus.COMPLETED, 0.95);
        when(reviewRepository.existsBySubmissionId(SUBMISSION_ID)).thenReturn(Boolean.TRUE);

        assertThat(service.approveEligible()).isZero();

        verify(reviewService, never()).autoApprove(any(), any());
    }

    @Test
    @DisplayName("ignores processing that has not completed")
    void skipsUnfinishedProcessing() {
        givenCandidate(ProcessingStatus.PROCESSING, 0.95);

        assertThat(service.approveEligible()).isZero();

        verify(reviewService, never()).autoApprove(any(), any());
    }

    private void givenCandidate(ProcessingStatus status, double maxConfidence) {
        when(processingApi.findProcessingByConfidence(0.9, 1.0))
                .thenReturn(List.of(new ProcessingModerationView(PROCESSING_ID, SUBMISSION_ID, status, maxConfidence)));
    }

    private static MarkSuggestionDto suggestion(UUID markId, double confidence) {
        MarkSuggestionDto dto = new MarkSuggestionDto();
        dto.setMarkId(markId);
        dto.setConfidence(confidence);
        return dto;
    }
}
