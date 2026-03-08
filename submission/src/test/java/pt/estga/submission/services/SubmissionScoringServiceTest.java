package pt.estga.submission.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pt.estga.content.entities.Monument;
import pt.estga.file.entities.MediaFile;
import pt.estga.submission.config.SubmissionDecisionProperties;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;
import pt.estga.user.entities.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubmissionScoringServiceTest {

    @Mock
    private SubmissionDecisionProperties properties;

    @Mock
    private MarkOccurrenceSubmissionRepository markOccurrenceSubmissionRepository;


    @InjectMocks
    private SubmissionScoringService scoringService;

    // ===== Credibility Score Tests =====

    @Test
    void calculateCredibilityScore_ShouldCalculateCorrectly_ForCompleteMarkOccurrenceProposal() {
        // Arrange
        when(properties.getBaseScoreAuthenticatedUser()).thenReturn(10);
        when(properties.getCompletenessScoreUserNotes()).thenReturn(5);
        when(properties.getCompletenessScoreLocation()).thenReturn(10);
        when(properties.getCompletenessScoreMediaFile()).thenReturn(10);
        when(properties.getMaxCredibilityScore()).thenReturn(100);

        User user = User.builder().id(1L).build();
        MediaFile mediaFile = MediaFile.builder().id(1L).build();
        
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .submittedBy(user)
                .userNotes("Some notes")
                .latitude(40.0)
                .longitude(-8.0)
                .originalMediaFile(mediaFile)
                .build();

        // Act
        Integer score = scoringService.calculateCredibilityScore(proposal);

        // Assert
        // 10 (User) + 5 (Notes) + 10 (Location) + 10 (Media) = 35
        assertEquals(35, score);
    }

    @Test
    void calculateCredibilityScore_ShouldCalculateCorrectly_ForMinimalMarkOccurrenceProposal() {
        // Arrange
        when(properties.getMaxCredibilityScore()).thenReturn(100);

        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .build();

        // Act
        Integer score = scoringService.calculateCredibilityScore(proposal);

        // Assert
        // 0 (No User) + 0 (No Notes) + 0 (No Location) + 0 (No Media) = 0
        assertEquals(0, score);
    }

    @Test
    void calculateCredibilityScore_ShouldCapScoreAtMax() {
        // Arrange
        when(properties.getBaseScoreAuthenticatedUser()).thenReturn(50);
        when(properties.getCompletenessScoreUserNotes()).thenReturn(60);
        when(properties.getMaxCredibilityScore()).thenReturn(100);
        
        User user = User.builder().id(1L).build();
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .submittedBy(user)
                .userNotes("Notes")
                .build();

        // Act
        Integer score = scoringService.calculateCredibilityScore(proposal);

        // Assert
        // 50 + 60 = 110, but max is 100
        assertEquals(100, score);
    }

    @Test
    void calculateCredibilityScore_WithLocationOnly() {
        // Arrange
        when(properties.getBaseScoreAuthenticatedUser()).thenReturn(10);
        when(properties.getCompletenessScoreLocation()).thenReturn(10);
        when(properties.getMaxCredibilityScore()).thenReturn(100);

        User user = User.builder().id(1L).build();
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .submittedBy(user)
                .latitude(40.0)
                .longitude(-8.0)
                .build();

        // Act
        Integer score = scoringService.calculateCredibilityScore(proposal);

        // Assert
        // 10 (User) + 10 (Location) = 20
        assertEquals(20, score);
    }

    // ===== Priority Calculation Tests =====

    @Test
    void calculatePriority_MinimalProposal_ReturnsOnlyCredibilityScore() {
        // Arrange - minimal proposal with no user
        when(properties.getMaxCredibilityScore()).thenReturn(100);
        when(properties.getReputationBoostPerApprovedProposal()).thenReturn(2);
        when(properties.getMaxReputationBoost()).thenReturn(40);
        when(properties.getNewMonumentProposalBoost()).thenReturn(5);

        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .build();

        // Act
        Integer priority = scoringService.calculatePriority(proposal);

        // Assert - should only have credibility score (0) since no other factors apply
        assertEquals(0, priority);
    }

    @Test
    void calculatePriority_CompleteProposalWithUserReputation() {
        // Arrange
        when(properties.getBaseScoreAuthenticatedUser()).thenReturn(10);
        when(properties.getCompletenessScoreUserNotes()).thenReturn(5);
        when(properties.getCompletenessScoreLocation()).thenReturn(10);
        when(properties.getCompletenessScoreMediaFile()).thenReturn(10);
        when(properties.getMaxCredibilityScore()).thenReturn(100);
        when(properties.getReputationBoostPerApprovedProposal()).thenReturn(2);
        when(properties.getMaxReputationBoost()).thenReturn(40);
        when(properties.getNewMonumentProposalBoost()).thenReturn(5);

        User user = User.builder().id(1L).build();
        MediaFile mediaFile = MediaFile.builder().id(1L).build();

        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .submittedBy(user)
                .userNotes("Good notes")
                .latitude(40.0)
                .longitude(-8.0)
                .originalMediaFile(mediaFile)
                .newMark(true)
                .build();

        // Mock stats - user has 10 approved proposals
        when(markOccurrenceSubmissionRepository.countAcceptedByUserId(1L)).thenReturn(10L);

        // Act
        Integer priority = scoringService.calculatePriority(proposal);

        // Assert
        // Credibility: 10 + 5 + 10 + 10 = 35
        // Reputation: 10 * 2 = 20 (10 approved proposals * 2 boost)
        // New monument: 5
        // Total: 35 + 20 + 5 = 60
        assertEquals(60, priority);
    }

    @Test
    void calculatePriority_WithMaxReputationBoost() {
        // Arrange
        when(properties.getBaseScoreAuthenticatedUser()).thenReturn(10);
        when(properties.getMaxCredibilityScore()).thenReturn(100);
        when(properties.getReputationBoostPerApprovedProposal()).thenReturn(2);
        when(properties.getMaxReputationBoost()).thenReturn(40);
        when(properties.getNewMonumentProposalBoost()).thenReturn(5);

        User user = User.builder().id(1L).build();

        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .submittedBy(user)
                .build();

        // Mock stats - user has 50 approved proposals (should cap at max boost)
        when(markOccurrenceSubmissionRepository.countAcceptedByUserId(1L)).thenReturn(50L);

        // Act
        Integer priority = scoringService.calculatePriority(proposal);

        // Assert
        // Credibility: 10 = 10
        // Reputation: min(50 * 2, 40) = 40 (capped at max)
        // New monument: 0 (existing monument)
        // Total: 10 + 40 = 50
        assertEquals(50, priority);
    }

    @Test
    void calculatePriority_NewMonumentBonus_AppliedForNewMark() {
        // Arrange
        when(properties.getBaseScoreAuthenticatedUser()).thenReturn(10);
        when(properties.getMaxCredibilityScore()).thenReturn(100);
        when(properties.getReputationBoostPerApprovedProposal()).thenReturn(2);
        when(properties.getMaxReputationBoost()).thenReturn(40);
        when(properties.getNewMonumentProposalBoost()).thenReturn(5);

        User user = User.builder().id(1L).build();
        Monument existingMonument = Monument.builder().id(1L).build();

        // New mark proposal (even with existing monument)
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .submittedBy(user)
                .existingMonument(existingMonument)
                .newMark(true) // Important: new mark despite existing monument
                .build();

        // Mock no prior approved proposals
        // Mock no prior approved proposals
        when(markOccurrenceSubmissionRepository.countAcceptedByUserId(1L)).thenReturn(0L);

        // Act
        Integer priority = scoringService.calculatePriority(proposal);

        // Assert
        // Credibility: 10
        // Reputation: 0 (triggered by newMark=true)
        // New monument: 5 (triggered by newMark=true)
        // Total: 10 + 0 + 5 = 15
        assertEquals(15, priority);
    }

    @Test
    void calculatePriority_NoNewMonumentBonus_ForExistingMonumentAndMark() {
        // Arrange
        when(properties.getBaseScoreAuthenticatedUser()).thenReturn(10);
        when(properties.getMaxCredibilityScore()).thenReturn(100);
        when(properties.getReputationBoostPerApprovedProposal()).thenReturn(2);
        when(properties.getMaxReputationBoost()).thenReturn(40);
        when(properties.getNewMonumentProposalBoost()).thenReturn(5);

        User user = User.builder().id(1L).build();
        Monument existingMonument = Monument.builder().id(1L).build();

        // Existing mark proposal (no bonus)
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .submittedBy(user)
                .existingMonument(existingMonument)
                .newMark(false)
                .build();

        // Mock no prior approved proposals
        when(markOccurrenceSubmissionRepository.countAcceptedByUserId(1L)).thenReturn(0L);

        // Act
        Integer priority = scoringService.calculatePriority(proposal);

        // Assert
        // Credibility: 10
        // Reputation: 0
        // New monument: 0 (not a new proposal)
        // Total: 10
        assertEquals(10, priority);
    }

    @Test
    void calculatePriority_WithoutUser_NoReputationBoost() {
        // Arrange
        when(properties.getMaxCredibilityScore()).thenReturn(100);
        when(properties.getReputationBoostPerApprovedProposal()).thenReturn(2);
        when(properties.getMaxReputationBoost()).thenReturn(40);
        when(properties.getNewMonumentProposalBoost()).thenReturn(5);

        // No user submitted
        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .submittedBy(null)
                .newMark(true)
                .build();

        // Act
        Integer priority = scoringService.calculatePriority(proposal);

        // Assert
        // Credibility: 0 (no user)
        // Reputation: 0 (no user)
        // New monument: 5
        // Total: 5
        assertEquals(5, priority);
    }

    @Test
    void calculatePriority_ExcellentProposal_WithAllFactors() {
        // Arrange
        when(properties.getBaseScoreAuthenticatedUser()).thenReturn(10);
        when(properties.getCompletenessScoreUserNotes()).thenReturn(5);
        when(properties.getCompletenessScoreLocation()).thenReturn(10);
        when(properties.getCompletenessScoreMediaFile()).thenReturn(10);
        when(properties.getMaxCredibilityScore()).thenReturn(100);
        when(properties.getReputationBoostPerApprovedProposal()).thenReturn(2);
        when(properties.getMaxReputationBoost()).thenReturn(40);
        when(properties.getNewMonumentProposalBoost()).thenReturn(5);

        User user = User.builder().id(1L).build();
        MediaFile mediaFile = MediaFile.builder().id(1L).build();

        MarkOccurrenceSubmission proposal = MarkOccurrenceSubmission.builder()
                .submittedBy(user)
                .userNotes("Excellent detailed notes")
                .latitude(40.0)
                .longitude(-8.0)
                .originalMediaFile(mediaFile)
                .newMark(true)
                .build();

        // Mock stats - user has 15 approved proposals
        when(markOccurrenceSubmissionRepository.countAcceptedByUserId(1L)).thenReturn(15L);

        // Act
        Integer priority = scoringService.calculatePriority(proposal);

        // Assert
        // Credibility: 10 + 5 + 10 + 10 = 35
        // Reputation: 15 * 2 = 30
        // New monument: 5
        // Total: 35 + 30 + 5 = 70
        assertEquals(70, priority);
    }
}
