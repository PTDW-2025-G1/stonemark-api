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
import pt.estga.submission.projections.ProposalStatsProjection;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;
import pt.estga.user.entities.User;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private MarkOccurrenceSubmissionRepository markOccurrenceSubmissionRepository;

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
        MarkOccurrenceSubmission submission = new MarkOccurrenceSubmission();
        submission.setId(id);
        when(markOccurrenceSubmissionRepository.findById(id)).thenReturn(Optional.of(submission));

        // Act
        Optional<MarkOccurrenceSubmission> result = proposalService.findById(id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void findByUser_ShouldReturnPageOfProposals() {
        // Arrange
        User user = User.builder().id(1L).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<MarkOccurrenceSubmission> page = new PageImpl<>(Collections.emptyList());
        when(markOccurrenceSubmissionRepository.findBySubmittedBy(user, pageable)).thenReturn(page);

        // Act
        Page<MarkOccurrenceSubmission> result = proposalService.findByUser(user, pageable);

        // Assert
        assertNotNull(result);
        verify(markOccurrenceSubmissionRepository).findBySubmittedBy(user, pageable);
    }

    @Test
    void getStatsByUser_ShouldReturnStats() {
        // Arrange
        User user = User.builder().id(1L).build();
        ProposalStatsProjection stats = mock(ProposalStatsProjection.class);
        when(markOccurrenceSubmissionRepository.getStatsByUserId(user.getId())).thenReturn(stats);

        // Act
        ProposalStatsProjection result = proposalService.getStatsByUser(user);

        // Assert
        assertNotNull(result);
        verify(markOccurrenceSubmissionRepository).getStatsByUserId(user.getId());
    }

    @Test
    void delete_ShouldDeleteProposalAndEvictCache() {
        // Arrange
        Long id = 1L;
        User user = User.builder().id(10L).build();
        MarkOccurrenceSubmission submission = new MarkOccurrenceSubmission();
        submission.setId(id);
        submission.setSubmittedBy(user);

        when(markOccurrenceSubmissionRepository.findById(id)).thenReturn(Optional.of(submission));
        when(cacheManager.getCache("proposalStats")).thenReturn(cache);

        // Act
        proposalService.delete(id);

        // Assert
        verify(markOccurrenceSubmissionRepository).delete(submission);
        verify(cache).evict(user.getId());
    }
}
