package pt.estga.decision.rules.markoccurrence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.rules.DecisionRuleResult;
import pt.estga.file.entities.MediaFile;
import pt.estga.proposal.entities.MarkOccurrenceProposal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CredibilityBasedRule decision rule.
 * This rule evaluates proposals based on their credibility score and data completeness.
 */
@ExtendWith(MockitoExtension.class)
class CredibilityBasedRuleTest {

    private final CredibilityBasedRule rule = new CredibilityBasedRule();

    @Test
    void evaluate_ShouldReturnNull_WhenCredibilityScoreIsNull() {
        // Arrange
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .credibilityScore(null)
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNull(result, "Should skip when credibility score is null");
    }

    @Test
    void evaluate_ShouldAccept_HighCredibilityWithCompleteData() {
        // Arrange
        MediaFile mediaFile = MediaFile.builder().id(1L).build();
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .credibilityScore(75) // Above high threshold (60)
                .latitude(40.0)
                .longitude(-8.0)
                .originalMediaFile(mediaFile)
                .userNotes("Good notes")
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.ACCEPT, result.outcome());
        assertTrue(result.confident());
        assertTrue(result.reason().contains("complete data"));
    }

    @Test
    void evaluate_ShouldAccept_HighCredibilityWithLocationAndNotes() {
        // Arrange - has location + notes but no media
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .credibilityScore(70)
                .latitude(40.0)
                .longitude(-8.0)
                .userNotes("Detailed notes")
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.ACCEPT, result.outcome());
        assertTrue(result.confident());
    }

    @Test
    void evaluate_ShouldAccept_HighCredibilityWithLocationAndMedia() {
        // Arrange - has location + media but no notes
        MediaFile mediaFile = MediaFile.builder().id(1L).build();
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .credibilityScore(65)
                .latitude(40.0)
                .longitude(-8.0)
                .originalMediaFile(mediaFile)
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.ACCEPT, result.outcome());
        assertTrue(result.confident());
    }

    @Test
    void evaluate_ShouldReturnNull_HighCredibilityWithoutLocation() {
        // Arrange - high credibility but no location (critical data missing)
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .credibilityScore(70)
                .userNotes("Good notes")
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNull(result, "Should skip without location data");
    }

    @Test
    void evaluate_ShouldReturnNull_HighCredibilityWithoutMediaAndNotes() {
        // Arrange - has location but no media/notes
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .credibilityScore(70)
                .latitude(40.0)
                .longitude(-8.0)
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNull(result, "Should skip without media or notes");
    }

    @Test
    void evaluate_ShouldReject_VeryLowCredibility() {
        // Arrange
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .credibilityScore(10) // Below low threshold (20)
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.REJECT, result.outcome());
        assertFalse(result.confident(), "Low credibility rejections are not confident");
        assertTrue(result.reason().contains("too low"));
    }

    @Test
    void evaluate_ShouldReject_ZeroCredibility() {
        // Arrange
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .credibilityScore(0)
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.REJECT, result.outcome());
    }

    @Test
    void evaluate_ShouldReturnNull_BorderlineLowCredibility() {
        // Arrange - borderline case (between 20-60, no acceptance)
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .credibilityScore(30)
                .latitude(40.0)
                .longitude(-8.0)
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNull(result, "Borderline credibility should let other rules decide");
    }

    @Test
    void evaluate_ShouldReturnNull_EdgeCaseHighThreshold() {
        // Arrange - exactly at threshold (60)
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .credibilityScore(60)
                .latitude(40.0)
                .longitude(-8.0)
                .userNotes("Notes")
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.ACCEPT, result.outcome());
    }

    @Test
    void evaluate_ShouldReturnNull_EdgeCaseLowThreshold() {
        // Arrange - exactly at threshold (20)
        MarkOccurrenceProposal proposal = MarkOccurrenceProposal.builder()
                .id(1L)
                .credibilityScore(20)
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNull(result, "At low threshold should let other rules decide");
    }

    @Test
    void getOrder_ShouldReturn15() {
        // Act & Assert
        assertEquals(15, rule.getOrder(), "CredibilityBasedRule should run before HighPriorityRule");
    }
}

