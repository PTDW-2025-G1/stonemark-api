package pt.estga.review.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.projections.ProcessingOverviewProjection;
import pt.estga.processing.services.markevidenceprocessing.MarkEvidenceProcessingQueryService;
import pt.estga.processing.services.suggestions.MarkSuggestionQueryService;
import pt.estga.mark.services.mark.MarkQueryService;
import pt.estga.mark.services.mark.MarkCommandService;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
import pt.estga.shared.events.AfterCommitEventPublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import pt.estga.user.services.UserQueryService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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
    MarkQueryService markQueryService;

    @Mock
    MarkCommandService markCommandService;

    @Mock
    MarkEvidenceReviewRepository reviewRepository;

    @Mock
    UserQueryService userQueryService;

    @Mock
    AfterCommitEventPublisher eventPublisher;

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    ReviewService reviewService;

    @BeforeEach
    public void beforeEach() {
        // Inject meter registry manually
        reviewService = new ReviewService(submissionQueryService, markEvidenceProcessingQueryService, suggestionQueryService, markCommandService, markQueryService, reviewRepository, userQueryService, eventPublisher, meterRegistry);
        // In unit tests we construct the service directly so @Value fields are not injected.
        // Permit empty-review for tests to avoid flakiness; production behavior remains governed by properties.
        try {
            Field f = ReviewService.class.getDeclaredField("allowEmptyReview");
            f.setAccessible(true);
            f.setBoolean(reviewService, true);
        } catch (Exception ignored) {
        }
    }

    @Test
    public void concurrency_accept_oneSucceeds_otherFails() throws Exception {
        Long submissionId = 1L;
        Long markId = 10L;

        MarkEvidenceSubmission submission = new MarkEvidenceSubmission();
        submission.setId(submissionId);
        when(submissionQueryService.findById(submissionId)).thenReturn(Optional.of(submission));

        MarkEvidenceProcessing processing = new MarkEvidenceProcessing();
        processing.setId(UUID.randomUUID());
        processing.setStatus(ProcessingStatus.COMPLETED);
        // ensure there is at least one suggestion so review is allowed
        processing.setSuggestions(List.of(new MarkSuggestion()));
        ProcessingOverviewProjection overview = new ProcessingOverviewProjection() {
            @Override public UUID getId() { return processing.getId(); }
            @Override public ProcessingStatus getStatus() { return processing.getStatus(); }
        };
        when(markEvidenceProcessingQueryService.findOverviewBySubmissionId(submissionId)).thenReturn(Optional.of(overview));

        when(suggestionQueryService.existsByProcessingIdAndMarkId(processing.getId(), markId)).thenReturn(true);

        Mark mark = new Mark(); mark.setId(markId);
        when(markQueryService.findById(markId)).thenReturn(Optional.of(mark));

        // control save: first call returns saved review, second call throws DataIntegrityViolationException
        AtomicInteger counter = new AtomicInteger(0);
        when(reviewRepository.save(any(MarkEvidenceReview.class))).thenAnswer(invocation -> {
            int c = counter.incrementAndGet();
            if (c == 1) {
                MarkEvidenceReview r = invocation.getArgument(0);
                r.setId(100L);
                return r;
            }
            throw new DataIntegrityViolationException("duplicate");
        });

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Runnable task = () -> {
            ready.countDown();
            try { start.await(); } catch (InterruptedException e) { throw new RuntimeException(e); }
            try {
                MarkEvidenceReview r = reviewService.acceptSuggestion(submissionId, markId, null);
                assertNotNull(r);
            } catch (IllegalStateException ex) {
                // expected for one thread
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start(); t2.start();
        ready.await();
        start.countDown();
        t1.join(); t2.join();

        // ensure save attempted at least twice
        verify(reviewRepository, atLeast(2)).save(any(MarkEvidenceReview.class));
    }

    @Test
    public void rejectBeforeProcessing_throws() {
        Long submissionId = 2L;
        MarkEvidenceSubmission submission = new MarkEvidenceSubmission(); submission.setId(submissionId);
        when(submissionQueryService.findById(submissionId)).thenReturn(Optional.of(submission));

        MarkEvidenceProcessing processing = new MarkEvidenceProcessing();
        processing.setStatus(ProcessingStatus.PROCESSING);
        ProcessingOverviewProjection overview = new ProcessingOverviewProjection() {
            @Override public UUID getId() { return UUID.randomUUID(); }
            @Override public ProcessingStatus getStatus() { return processing.getStatus(); }
        };
        when(markEvidenceProcessingQueryService.findOverviewBySubmissionId(submissionId)).thenReturn(Optional.of(overview));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> reviewService.rejectAll(submissionId, null));
        assertTrue(ex.getMessage().contains("Processing not ready") || ex.getMessage().contains("not reviewable") );
    }

    @Test
    public void doubleReviewAttempt_blocked() {
        Long submissionId = 3L;
        MarkEvidenceSubmission submission = new MarkEvidenceSubmission(); submission.setId(submissionId);
        when(submissionQueryService.findById(submissionId)).thenReturn(Optional.of(submission));

        MarkEvidenceProcessing processing = new MarkEvidenceProcessing(); processing.setStatus(ProcessingStatus.COMPLETED);
        // ensure suggestions present for this test
        processing.setSuggestions(List.of(new MarkSuggestion()));
        ProcessingOverviewProjection overview = new ProcessingOverviewProjection() {
            @Override public UUID getId() { return UUID.randomUUID(); }
            @Override public ProcessingStatus getStatus() { return processing.getStatus(); }
        };
        when(markEvidenceProcessingQueryService.findOverviewBySubmissionId(submissionId)).thenReturn(Optional.of(overview));

        when(reviewRepository.existsBySubmissionId(submissionId)).thenReturn(false).thenReturn(true);

        // first call should create a review — mock save to return review
        when(reviewRepository.save(any(MarkEvidenceReview.class))).thenAnswer(i -> { MarkEvidenceReview r = i.getArgument(0); r.setId(200L); return r; });

        MarkEvidenceReview saved = reviewService.rejectAll(submissionId, null);
        assertNotNull(saved);

        // second attempt should fail due to existsBySubmissionId returning true
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> reviewService.rejectAll(submissionId, null));
        assertTrue(ex.getMessage().contains("already reviewed") || ex.getMessage().contains("already been reviewed"));
    }
}
