package pt.estga.submission.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.entities.Submission;
import pt.estga.submission.projections.ProposalStatsProjection;
import pt.estga.submission.repositories.SubmissionRepository;
import pt.estga.user.entities.User;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private SubmissionRepository<Submission> submissionRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private SubmissionServiceImpl proposalService;

    @Test
    void findById_ShouldReturnProposal_WhenExists() {
        // Arrange
        Long id = 1L;
        Submission submission = new MarkOccurrenceSubmission();
        submission.setId(id);
        when(submissionRepository.findById(id)).thenReturn(Optional.of(submission));

        // Act
        Optional<Submission> result = proposalService.findById(id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void findByUser_ShouldReturnPageOfProposals() {
        // Arrange
        User user = User.builder().id(1L).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Submission> page = new PageImpl<>(Collections.emptyList());
        when(submissionRepository.findBySubmittedBy(user, pageable)).thenReturn(page);

        // Act
        Page<Submission> result = proposalService.findByUser(user, pageable);

        // Assert
        assertNotNull(result);
        verify(submissionRepository).findBySubmittedBy(user, pageable);
    }

    @Test
    void getStatsByUser_ShouldReturnStats() {
        // Arrange
        User user = User.builder().id(1L).build();
        ProposalStatsProjection stats = mock(ProposalStatsProjection.class);
        when(submissionRepository.getStatsByUserId(user.getId())).thenReturn(stats);

        // Act
        ProposalStatsProjection result = proposalService.getStatsByUser(user);

        // Assert
        assertNotNull(result);
        verify(submissionRepository).getStatsByUserId(user.getId());
    }

    @Test
    void delete_ShouldDeleteProposalAndEvictCache() {
        // Arrange
        Long id = 1L;
        User user = User.builder().id(10L).build();
        Submission submission = new MarkOccurrenceSubmission();
        submission.setId(id);
        submission.setSubmittedBy(user);

        when(submissionRepository.findById(id)).thenReturn(Optional.of(submission));
        when(cacheManager.getCache("proposalStats")).thenReturn(cache);

        // Act
        proposalService.delete(id);

        // Assert
        verify(submissionRepository).delete(submission);
        verify(cache).evict(user.getId());
    }
}
