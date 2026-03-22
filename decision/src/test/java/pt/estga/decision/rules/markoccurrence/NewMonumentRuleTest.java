package pt.estga.decision.rules.markoccurrence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.rules.DecisionRuleResult;
import pt.estga.file.entities.MediaFile;
import pt.estga.monument.Monument;
import pt.estga.submission.config.SubmissionDecisionProperties;
import pt.estga.submission.entities.MarkOccurrenceSubmission;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for the NewMonumentRule decision rule.
 * This rule handles proposals for new monuments/marks with special logic.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NewMonumentRuleTest {

    @Mock
    private SubmissionDecisionProperties properties;

    @InjectMocks
    private NewMonumentRule rule;

    @Test
    void evaluate_ShouldReturnNull_ForExistingMonumentAndMark() {
        // Arrange - neither new monument nor new mark
        Monument existingMonument = Monument.builder().id(1L).build();
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .existingMonument(existingMonument)
                .newMark(false)
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNull(result, "Should skip for existing monument/mark");
    }

    @Test
    void evaluate_ShouldSendToReview_WhenManualReviewRequired() {
        // Arrange
        when(properties.getRequireManualReviewForNewMonuments()).thenReturn(true);

        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .credibilityScore(50)
                .existingMonument(null) // New monument
                .newMark(true)
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.INCONCLUSIVE, result.outcome());
        assertTrue(result.reason().contains("manual review"));
    }

    @Test
    void evaluate_ShouldAutoAccept_ExceptionalQualityNewMonument() {
        // Arrange
        when(properties.getRequireManualReviewForNewMonuments()).thenReturn(true);

        MediaFile mediaFile = MediaFile.builder().id(1L).build();
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .credibilityScore(85) // Above acceptance threshold (80)
                .existingMonument(null)
                .newMark(true)
                .latitude(40.0)
                .longitude(-8.0)
                .originalMediaFile(mediaFile)
                .userNotes("Excellent notes")
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.ACCEPT, result.outcome());
        assertTrue(result.confident());
        assertTrue(result.reason().contains("exceptional quality"));
    }

    @Test
    void evaluate_ShouldSendToReview_HighQualityButIncompleteData() {
        // Arrange
        when(properties.getRequireManualReviewForNewMonuments()).thenReturn(true);

        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .credibilityScore(85)
                .existingMonument(null)
                .newMark(true)
                .latitude(40.0)
                // Missing longitude
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.INCONCLUSIVE, result.outcome());
        assertTrue(result.reason().contains("manual review"));
    }

    @Test
    void evaluate_ShouldReturnNull_WhenManualReviewNotRequired() {
        // Arrange
        when(properties.getRequireManualReviewForNewMonuments()).thenReturn(false);

        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .credibilityScore(50)
                .existingMonument(null) // New monument
                .newMark(true)
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNull(result, "Should skip if manual review not required");
    }

    @Test
    void evaluate_ShouldHandleNewMark_WithExistingMonument() {
        // Arrange - new mark but existing monument
        when(properties.getRequireManualReviewForNewMonuments()).thenReturn(true);

        Monument existingMonument = Monument.builder().id(1L).build();
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .credibilityScore(50)
                .existingMonument(existingMonument) // Has monument
                .newMark(true) // But new mark
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.INCONCLUSIVE, result.outcome());
    }

    @Test
    void evaluate_ShouldRequireAllData_ForAutoAcceptance() {
        // Arrange
        when(properties.getRequireManualReviewForNewMonuments()).thenReturn(true);

        // Missing media
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .credibilityScore(85)
                .existingMonument(null)
                .newMark(true)
                .latitude(40.0)
                .longitude(-8.0)
                .userNotes("Good notes")
                // Missing originalMediaFile
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.INCONCLUSIVE, result.outcome());
    }

    @Test
    void evaluate_ShouldRequireLocationAndData_ForAutoAcceptance() {
        // Arrange
        when(properties.getRequireManualReviewForNewMonuments()).thenReturn(true);

        // Missing latitude
        MediaFile mediaFile = MediaFile.builder().id(1L).build();
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .credibilityScore(85)
                .existingMonument(null)
                .newMark(true)
                .longitude(-8.0)
                .originalMediaFile(mediaFile)
                .userNotes("Good notes")
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.INCONCLUSIVE, result.outcome());
    }

    @Test
    void evaluate_EdgeCase_ExactlyAtThreshold() {
        // Arrange
        when(properties.getRequireManualReviewForNewMonuments()).thenReturn(true);

        MediaFile mediaFile = MediaFile.builder().id(1L).build();
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .id(1L)
                .credibilityScore(80) // Exactly at threshold
                .existingMonument(null)
                .newMark(true)
                .latitude(40.0)
                .longitude(-8.0)
                .originalMediaFile(mediaFile)
                .userNotes("Notes")
                .build();

        // Act
        DecisionRuleResult result = rule.evaluate(proposal);

        // Assert
        assertNotNull(result);
        assertEquals(DecisionOutcome.ACCEPT, result.outcome());
    }

    @Test
    void getOrder_ShouldReturn10() {
        // Act & Assert
        assertEquals(10, rule.getOrder(), "NewMonumentRule should run first");
    }
}

