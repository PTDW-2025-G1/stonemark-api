package pt.estga.decision.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pt.estga.decision.entities.SubmissionDecisionAttempt;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.enums.DecisionType;
import pt.estga.decision.repositories.SubmissionDecisionAttemptRepository;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;
import pt.estga.user.entities.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionDecisionServiceTest {

    @Mock
    private SubmissionDecisionAttemptRepository attemptRepo;

    @Mock
    private MarkOccurrenceSubmissionRepository proposalRepo;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    // Concrete implementation for testing abstract class
    private static class TestSubmissionDecisionService extends SubmissionDecisionService {
        public TestSubmissionDecisionService(
                SubmissionDecisionAttemptRepository attemptRepo,
                MarkOccurrenceSubmissionRepository proposalRepo,
                ApplicationEventPublisher eventPublisher
        ) {
            super(attemptRepo, proposalRepo, eventPublisher);
        }

        @Override
        public SubmissionDecisionAttempt makeAutomaticDecision(MarkOccurrenceSubmission proposal) {
            return null; // Not testing this here
        }

        @Override
        protected void publishAcceptedEvent(MarkOccurrenceSubmission proposal) {
            // No-op for base test
        }
    }

    private TestSubmissionDecisionService service;

    @BeforeEach
    void setUp() {
        service = new TestSubmissionDecisionService(attemptRepo, proposalRepo, eventPublisher);
    }

    @Test
    void makeManualDecision_ShouldUpdateStatusAndSaveAttempt() {
        // Arrange
        Long proposalId = 1L;
        User moderator = User.builder().id(10L).build();
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder().id(proposalId).status(SubmissionStatus.UNDER_REVIEW).build();

        when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));

        // Act
        SubmissionDecisionAttempt result = service.makeManualDecision(proposalId, DecisionOutcome.ACCEPT, "Looks good", moderator);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.ACCEPT, result.getOutcome());
        assertEquals(DecisionType.MANUAL, result.getType());
        assertEquals(moderator, result.getDecidedBy());
        
        assertEquals(SubmissionStatus.MANUALLY_ACCEPTED, proposal.getStatus());
        verify(attemptRepo).save(any(SubmissionDecisionAttempt.class));
        verify(proposalRepo).save(proposal);
    }

    @Test
    void deactivateDecision_ShouldRevertStatus_WhenActive() {
        // Arrange
        Long proposalId = 1L;
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(proposalId)
                .status(SubmissionStatus.MANUALLY_ACCEPTED)
                .build();

        when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));

        // Act
        service.deactivateDecision(proposalId);

        // Assert
        assertEquals(SubmissionStatus.UNDER_REVIEW, proposal.getStatus());
        verify(proposalRepo).save(proposal);
    }

    @Test
    void deactivateDecision_ShouldDoNothing_WhenAlreadyUnderReview() {
        // Arrange
        Long proposalId = 1L;
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(proposalId)
                .status(SubmissionStatus.UNDER_REVIEW)
                .build();

        when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));

        // Act
        service.deactivateDecision(proposalId);

        // Assert
        assertEquals(SubmissionStatus.UNDER_REVIEW, proposal.getStatus());
        verify(proposalRepo, never()).save(proposal);
    }

    @Test
    void activateDecision_ShouldApplyOldDecision() {
        // Arrange
        Long attemptId = 100L;
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder().id(1L).build();
        SubmissionDecisionAttempt attempt = SubmissionDecisionAttempt.builder()
                .id(attemptId)
                .submission(proposal)
                .type(DecisionType.MANUAL)
                .outcome(DecisionOutcome.REJECT)
                .build();

        when(attemptRepo.findById(attemptId)).thenReturn(Optional.of(attempt));

        // Act
        service.activateDecision(attemptId);

        // Assert
        assertEquals(SubmissionStatus.MANUALLY_REJECTED, proposal.getStatus());
        verify(proposalRepo).save(proposal);
    }
}
