package pt.estga.decision.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import pt.estga.decision.entities.SubmissionDecisionAttempt;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.enums.DecisionType;
import pt.estga.decision.repositories.SubmissionDecisionAttemptRepository;
import pt.estga.decision.rules.DecisionRule;
import pt.estga.decision.rules.DecisionRuleResult;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.events.SubmissionAcceptedEvent;
import pt.estga.submission.repositories.MarkOccurrenceProposalRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarkOccurrenceSubmissionDecisionServiceTest {

    @Mock
    private SubmissionDecisionAttemptRepository attemptRepo;

    @Mock
    private MarkOccurrenceProposalRepository proposalRepo;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DecisionRule<MarkOccurrenceSubmission> rule1;

    @Mock
    private DecisionRule<MarkOccurrenceSubmission> rule2;

    @InjectMocks
    private MarkOccurrenceProposalDecisionService decisionService;

    @Test
    void makeAutomaticDecision_ShouldApplyFirstMatchingRule() {
        // Arrange
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder().id(1L).build();
        
        // Setup rules via constructor injection simulation (since we can't easily inject list into @InjectMocks)
        // We need to manually construct the service to inject the list of rules
        List<DecisionRule<MarkOccurrenceSubmission>> rules = List.of(rule1, rule2);
        decisionService = new MarkOccurrenceProposalDecisionService(attemptRepo, proposalRepo, eventPublisher, rules);

        when(rule1.getOrder()).thenReturn(10);
        when(rule2.getOrder()).thenReturn(20);

        // Rule 1 matches
        when(rule1.evaluate(proposal)).thenReturn(DecisionRuleResult.conclusive(DecisionOutcome.ACCEPT, true, "Rule 1 matched"));

        // Act
        SubmissionDecisionAttempt result = decisionService.makeAutomaticDecision(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.ACCEPT, result.getOutcome());
        assertEquals(DecisionType.AUTOMATIC, result.getType());
        assertTrue(result.getConfident());
        assertEquals("Rule 1 matched", result.getNotes());

        verify(rule2, never()).evaluate(any()); // Should stop after first match
        verify(attemptRepo).save(any(SubmissionDecisionAttempt.class));
        verify(proposalRepo).save(proposal);
        assertEquals(SubmissionStatus.AUTO_ACCEPTED, proposal.getStatus());
        verify(eventPublisher).publishEvent(any(SubmissionAcceptedEvent.class));
    }

    @Test
    void makeAutomaticDecision_ShouldDefaultToInconclusive_WhenNoRuleMatches() {
        // Arrange
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder().id(1L).build();
        List<DecisionRule<MarkOccurrenceSubmission>> rules = List.of(rule1);
        decisionService = new MarkOccurrenceProposalDecisionService(attemptRepo, proposalRepo, eventPublisher, rules);

        when(rule1.getOrder()).thenReturn(10);
        when(rule1.evaluate(proposal)).thenReturn(null); // No match

        // Act
        SubmissionDecisionAttempt result = decisionService.makeAutomaticDecision(proposal);

        // Assert
        assertEquals(DecisionOutcome.INCONCLUSIVE, result.getOutcome());
        assertFalse(result.getConfident());
        assertEquals(SubmissionStatus.UNDER_REVIEW, proposal.getStatus());
    }

    @Test
    void makeAutomaticDecision_ShouldHandleRejection() {
        // Arrange
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder().id(1L).build();
        List<DecisionRule<MarkOccurrenceSubmission>> rules = List.of(rule1);
        decisionService = new MarkOccurrenceProposalDecisionService(attemptRepo, proposalRepo, eventPublisher, rules);

        when(rule1.getOrder()).thenReturn(10);
        when(rule1.evaluate(proposal)).thenReturn(DecisionRuleResult.conclusive(DecisionOutcome.REJECT, true, "Rejected"));

        // Act
        decisionService.makeAutomaticDecision(proposal);

        // Assert
        assertEquals(SubmissionStatus.AUTO_REJECTED, proposal.getStatus());
        verify(eventPublisher, never()).publishEvent(any(SubmissionAcceptedEvent.class)); // Should NOT publish accepted event
    }
}
