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
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.entities.MonumentProposal;
import pt.estga.proposal.enums.ProposalStatus;
import pt.estga.proposal.repositories.ProposalRepository;
import pt.estga.user.entities.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalDecisionServiceTest {

    @Mock
    private ProposalDecisionAttemptRepository attemptRepo;

    @Mock
    private ProposalRepository<MarkOccurrenceProposal> proposalRepo;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    // Concrete implementation for testing abstract class
    private static class TestProposalDecisionService extends ProposalDecisionService<MarkOccurrenceProposal> {
        public TestProposalDecisionService(
                ProposalDecisionAttemptRepository attemptRepo,
                ProposalRepository<MarkOccurrenceProposal> proposalRepo,
                ApplicationEventPublisher eventPublisher
        ) {
            super(attemptRepo, proposalRepo, eventPublisher, MarkOccurrenceProposal.class);
        }

        @Override
        public ProposalDecisionAttempt makeAutomaticDecision(MarkOccurrenceProposal proposal) {
            return null; // Not testing this here
        }

        @Override
        protected void publishAcceptedEvent(MarkOccurrenceProposal proposal) {
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
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder().id(proposalId).status(ProposalStatus.UNDER_REVIEW).build();

        when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));

        // Act
        ProposalDecisionAttempt result = service.makeManualDecision(proposalId, DecisionOutcome.ACCEPT, "Looks good", moderator);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.ACCEPT, result.getOutcome());
        assertEquals(DecisionType.MANUAL, result.getType());
        assertEquals(moderator, result.getDecidedBy());
        
        assertEquals(ProposalStatus.MANUALLY_ACCEPTED, proposal.getStatus());
        verify(attemptRepo).save(any(ProposalDecisionAttempt.class));
        verify(proposalRepo).save(proposal);
    }

    @Test
    void deactivateDecision_ShouldRevertStatus_WhenActive() {
        // Arrange
        Long proposalId = 1L;
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(proposalId)
                .status(ProposalStatus.MANUALLY_ACCEPTED)
                .build();

        when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));

        // Act
        service.deactivateDecision(proposalId);

        // Assert
        assertEquals(ProposalStatus.UNDER_REVIEW, proposal.getStatus());
        verify(proposalRepo).save(proposal);
    }

    @Test
    void deactivateDecision_ShouldDoNothing_WhenAlreadyUnderReview() {
        // Arrange
        Long proposalId = 1L;
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(proposalId)
                .status(ProposalStatus.UNDER_REVIEW)
                .build();

        when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));

        // Act
        service.deactivateDecision(proposalId);

        // Assert
        assertEquals(ProposalStatus.UNDER_REVIEW, proposal.getStatus());
        verify(proposalRepo, never()).save(proposal);
    }

    @Test
    void activateDecision_ShouldApplyOldDecision() {
        // Arrange
        Long attemptId = 100L;
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder().id(1L).build();
        ProposalDecisionAttempt attempt = ProposalDecisionAttempt.builder()
                .id(attemptId)
                .proposal(proposal)
                .type(DecisionType.MANUAL)
                .outcome(DecisionOutcome.REJECT)
                .build();

        when(attemptRepo.findById(attemptId)).thenReturn(Optional.of(attempt));

        // Act
        service.activateDecision(attemptId);

        // Assert
        assertEquals(ProposalStatus.MANUALLY_REJECTED, proposal.getStatus());
        verify(proposalRepo).save(proposal);
    }

    @Test
    void activateDecision_ShouldThrow_IfProposalTypeMismatch() {
        // Arrange
        Long attemptId = 100L;
        MonumentProposal otherProposal = MonumentProposal.builder().id(1L).build();
        ProposalDecisionAttempt attempt = ProposalDecisionAttempt.builder()
                .id(attemptId)
                .proposal(otherProposal)
                .build();

        when(attemptRepo.findById(attemptId)).thenReturn(Optional.of(attempt));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.activateDecision(attemptId));
    }
}
