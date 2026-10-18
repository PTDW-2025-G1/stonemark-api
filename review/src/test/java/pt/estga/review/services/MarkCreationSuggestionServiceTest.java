package pt.estga.review.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.estga.intake.api.IntakeSubmissionApi;
import pt.estga.mark.api.MarkQueryService;
import pt.estga.processing.api.ProcessingReviewApi;
import pt.estga.processing.api.SubmissionEmbedding;
import pt.estga.review.entities.MarkCreationSuggestion;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.MarkCreationSuggestionStatus;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.repositories.MarkCreationSuggestionRepository;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkCreationSuggestionServiceTest {

    private static final UUID SUB_A = UUID.randomUUID();
    private static final UUID SUB_B = UUID.randomUUID();
    private static final UUID SUB_C = UUID.randomUUID();

    @Mock
    private MarkEvidenceReviewRepository reviewRepository;
    @Mock
    private MarkCreationSuggestionRepository suggestionRepository;
    @Mock
    private ProcessingReviewApi processingApi;
    @Mock
    private IntakeSubmissionApi intakeApi;
    @Mock
    private MarkQueryService markQueryService;

    private MarkCreationSuggestionService service;

    @BeforeEach
    void setUp() {
        service = new MarkCreationSuggestionService(
                reviewRepository, suggestionRepository, processingApi, intakeApi, markQueryService);
        ReflectionTestUtils.setField(service, "minClusterSize", 3);
        ReflectionTestUtils.setField(service, "maxDistance", 0.15);
    }

    @Test
    @DisplayName("raises a suggestion for a cluster that reaches the minimum size")
    void raisesSuggestionForNewCluster() {
        givenCandidates(SUB_A, SUB_B, SUB_C);
        when(suggestionRepository.findByStatus(MarkCreationSuggestionStatus.OPEN)).thenReturn(List.of());
        when(suggestionRepository.findByStatus(MarkCreationSuggestionStatus.DISMISSED)).thenReturn(List.of());

        service.recompute();

        verify(suggestionRepository).save(any(MarkCreationSuggestion.class));
    }

    @Test
    @DisplayName("does not re-raise a cluster a moderator dismissed, even after it grew")
    void doesNotReRaiseDismissedCluster() {
        givenCandidates(SUB_A, SUB_B, SUB_C);
        when(suggestionRepository.findByStatus(MarkCreationSuggestionStatus.OPEN)).thenReturn(List.of());
        when(suggestionRepository.findByStatus(MarkCreationSuggestionStatus.DISMISSED))
                .thenReturn(List.of(dismissed(SUB_A, SUB_B)));

        service.recompute();

        verify(suggestionRepository, never()).save(any(MarkCreationSuggestion.class));
    }

    private void givenCandidates(UUID... submissionIds) {
        when(reviewRepository.findByDecision(ReviewDecision.NO_MATCH))
                .thenReturn(List.of(submissionIds).stream().map(MarkCreationSuggestionServiceTest::candidate).toList());
        when(processingApi.findEmbeddings(anyCollection()))
                .thenReturn(List.of(submissionIds).stream().map(MarkCreationSuggestionServiceTest::embedding).toList());
    }

    private static MarkEvidenceReview candidate(UUID submissionId) {
        return MarkEvidenceReview.builder()
                .submissionId(submissionId)
                .decision(ReviewDecision.NO_MATCH)
                .build();
    }

    private static SubmissionEmbedding embedding(UUID submissionId) {
        return new SubmissionEmbedding(submissionId, new float[]{1f, 0f, 0f});
    }

    private static MarkCreationSuggestion dismissed(UUID... memberIds) {
        return MarkCreationSuggestion.builder()
                .status(MarkCreationSuggestionStatus.DISMISSED)
                .memberSubmissionIds(new LinkedHashSet<>(Set.of(memberIds)))
                .build();
    }
}
