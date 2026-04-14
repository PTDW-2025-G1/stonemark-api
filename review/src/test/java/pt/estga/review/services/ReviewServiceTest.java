package pt.estga.review.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.projections.ProcessingOverviewProjection;
import pt.estga.processing.services.markevidenceprocessing.MarkEvidenceProcessingQueryService;
import pt.estga.processing.services.suggestions.MarkSuggestionQueryService;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.models.ResolutionResult;
import pt.estga.review.processors.ReviewProcessor;
import pt.estga.review.services.markevidencereview.MarkEvidenceReviewQueryService;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    MarkEvidenceSubmissionQueryService submissionQueryService;

    @Mock
    MarkEvidenceProcessingQueryService markEvidenceProcessingQueryService;

    @Mock
    MarkSuggestionQueryService suggestionQueryService;

    @Mock
    ReviewExecutor executor;

    @Mock
    ReviewProcessor processor;

    @Mock
    MarkEvidenceReviewQueryService markEvidenceReviewQueryService;

    ReviewService reviewService;

    @BeforeEach
    public void beforeEach() throws Exception {
        // Ensure the processor returns a type for the stream filter to work.
        // Use lenient() so Mockito doesn't complain if a test fails before this stub is used.
        lenient().when(processor.getSupportedType()).thenReturn(ReviewType.REJECTION);

        reviewService = new ReviewService(
                submissionQueryService,
                markEvidenceProcessingQueryService,
                suggestionQueryService,
                markEvidenceReviewQueryService,
                List.of(processor),
                executor
        );

        // In unit tests we construct the service directly so @Value fields are not injected.
        // Permit empty-review for tests to avoid flakiness; production behavior remains governed by properties.
        setPrivateField(reviewService, "allowEmptyReview", true);
        // No direct injection of MarkQueryService needed; processors are mocked in tests.
    }

    @Test
    public void concurrency_accept_oneSucceeds_otherFails() throws Exception {
        Long submissionId = 1L;
        UUID procId = UUID.randomUUID();

        // Setup Submission
        MarkEvidenceSubmission submission = new MarkEvidenceSubmission();
        submission.setId(submissionId);
        when(submissionQueryService.findById(submissionId)).thenReturn(Optional.of(submission));

        // Setup Processing Overview
        ProcessingOverviewProjection overview = mock(ProcessingOverviewProjection.class);
        when(overview.getId()).thenReturn(procId);
        when(overview.getStatus()).thenReturn(ProcessingStatus.COMPLETED);
        when(markEvidenceProcessingQueryService.findOverviewBySubmissionId(submissionId)).thenReturn(Optional.of(overview));

        // Processor logic: allow resolution (REJECTION flows return empty resolution)
        when(processor.resolve(eq(submissionId), any())).thenReturn(new ResolutionResult(null, null));

        // FIXED ANSWER LOGIC: Index 0 is Submission, not Review!
        AtomicInteger counter = new AtomicInteger(0);
        when(executor.execute(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            // reference the invocation to avoid unused-parameter warnings
            Objects.requireNonNull(invocation.getArgument(0));
            if (counter.incrementAndGet() == 1) {
                MarkEvidenceReview r = new MarkEvidenceReview();
                r.setId(100L);
                return r;
            }
            throw new DataIntegrityViolationException("Duplicate Key");
        });

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Runnable task = () -> {
            ready.countDown();
            try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            try {
                // Testing via rejectAll which uses ReviewType.REJECTION
                reviewService.rejectAll(submissionId, "Comment");
                successCount.incrementAndGet();
            } catch (IllegalStateException ex) {
                if (ex.getMessage().contains("already reviewed")) {
                    failureCount.incrementAndGet();
                }
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start(); t2.start();
        ready.await();
        start.countDown();
        t1.join(); t2.join();

        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());
    }

    @Test
    public void doubleReviewAttempt_blocked() {
        Long submissionId = 3L;
        when(submissionQueryService.findById(submissionId)).thenReturn(Optional.of(new MarkEvidenceSubmission()));

        ProcessingOverviewProjection overview = mock(ProcessingOverviewProjection.class);
        when(overview.getId()).thenReturn(UUID.randomUUID());
        when(overview.getStatus()).thenReturn(ProcessingStatus.COMPLETED);
        when(markEvidenceProcessingQueryService.findOverviewBySubmissionId(submissionId)).thenReturn(Optional.of(overview));

        // Allow processor to resolve
        when(processor.resolve(anyLong(), any())).thenReturn(new ResolutionResult(null, null));

        // Mock state: first check false, second check true
        when(markEvidenceReviewQueryService.existsBySubmissionId(submissionId))
                .thenReturn(false)
                .thenReturn(true);

        // First one works
        reviewService.rejectAll(submissionId, "first");

        // Second one throws before reaching the processor/executor
        assertThrows(IllegalStateException.class, () -> reviewService.rejectAll(submissionId, "second"));
    }

    @Test
    public void acceptAsNew_highConfidenceSuggestion_throws() throws Exception {
        Long submissionId = 5L;
        UUID procId = UUID.randomUUID();

        // Mocking the threshold at 0.5
        setPrivateField(reviewService, "newMarkMaxSuggestionConfidence", 0.5);

        ProcessingOverviewProjection overview = mock(ProcessingOverviewProjection.class);
        when(overview.getId()).thenReturn(procId);
        when(markEvidenceProcessingQueryService.findOverviewBySubmissionId(submissionId))
                .thenReturn(Optional.of(overview));

        // High confidence (0.9 > 0.5)
        when(suggestionQueryService.findMaxConfidenceByProcessingId(procId)).thenReturn(0.9);

        assertThrows(IllegalStateException.class, () ->
                reviewService.acceptAsNew(submissionId, "New Mark", "Comment"));
    }

    @Test
    public void acceptSuggestion_callsMatchProcessor() {
        Long submissionId = 6L;
        Long markId = 50L;
        UUID procId = UUID.randomUUID();

        // 1. Setup Data
        when(submissionQueryService.findById(submissionId)).thenReturn(Optional.of(new MarkEvidenceSubmission()));

        ProcessingOverviewProjection overview = mock(ProcessingOverviewProjection.class);
        when(overview.getId()).thenReturn(procId);
        when(overview.getStatus()).thenReturn(ProcessingStatus.COMPLETED);
        when(markEvidenceProcessingQueryService.findOverviewBySubmissionId(submissionId)).thenReturn(Optional.of(overview));

        Mark mark = new Mark();

        // 2. Mock Processor as MATCH
        lenient().when(processor.getSupportedType()).thenReturn(ReviewType.MATCH);
        ResolutionResult result = new ResolutionResult(mark, null);
        when(processor.resolve(eq(submissionId), any())).thenReturn(result);

        // 3. Execute
        reviewService.acceptSuggestion(submissionId, markId, "Matching it");

        // 4. Verify Executor received the resolved mark
        verify(executor).execute(any(), eq(ReviewDecision.APPROVED), eq("Matching it"), eq(result), eq(procId));
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
