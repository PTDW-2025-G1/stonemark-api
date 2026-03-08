package pt.estga.decision.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pt.estga.decision.entities.ProposalDecisionAttempt;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.enums.DecisionType;
import pt.estga.decision.repositories.ProposalDecisionAttemptRepository;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.repositories.ProposalRepository;
import pt.estga.user.entities.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionDecisionServiceTest {

    @Mock
    private ProposalDecisionAttemptRepository attemptRepo;

    @Mock
    private ProposalRepository<MarkOccurrenceSubmission> proposalRepo;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    // Concrete implementation for testing abstract class
    private static class TestProposalDecisionService extends ProposalDecisionService<MarkOccurrenceSubmission> {
        public TestProposalDecisionService(
                ProposalDecisionAttemptRepository attemptRepo,
                ProposalRepository<MarkOccurrenceSubmission> proposalRepo,
                ApplicationEventPublisher eventPublisher
        ) {
            super(attemptRepo, proposalRepo, eventPublisher, MarkOccurrenceSubmission.class);
        }

        @Override
        public ProposalDecisionAttempt makeAutomaticDecision(MarkOccurrenceSubmission proposal) {
            return null; // Not testing this here
        }

        @Override
        protected void publishAcceptedEvent(MarkOccurrenceSubmission proposal) {
            // No-op for base test
        }
    }

    private TestProposalDecisionService service;

    @BeforeEach
    void setUp() {
        service = new TestProposalDecisionService(attemptRepo, proposalRepo, eventPublisher);
    }

    @Test
    void makeManualDecision_ShouldUpdateStatusAndSaveAttempt() {
        // Arrange
        Long proposalId = 1L;
        User moderator = User.builder().id(10L).build();
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder().id(proposalId).status(SubmissionStatus.UNDER_REVIEW).build();

        when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));

        // Act
        ProposalDecisionAttempt result = service.makeManualDecision(proposalId, DecisionOutcome.ACCEPT, "Looks good", moderator);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.ACCEPT, result.getOutcome());
        assertEquals(DecisionType.MANUAL, result.getType());
        assertEquals(moderator, result.getDecidedBy());
        
        assertEquals(SubmissionStatus.MANUALLY_ACCEPTED, proposal.getStatus());
        verify(attemptRepo).save(any(ProposalDecisionAttempt.class));
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
        ProposalDecisionAttempt attempt = ProposalDecisionAttempt.builder()
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
