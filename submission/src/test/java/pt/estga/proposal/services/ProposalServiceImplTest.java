package pt.estga.proposal.services;

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
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.projections.ProposalStatsProjection;
import pt.estga.proposal.repositories.ProposalRepository;
import pt.estga.user.entities.User;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalServiceImplTest {

    @Mock
    private ProposalRepository<Proposal> proposalRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private ProposalServiceImpl proposalService;

    @Test
    void findById_ShouldReturnProposal_WhenExists() {
        // Arrange
        Long id = 1L;
        Proposal proposal = new MarkOccurrenceProposal();
        proposal.setId(id);
        when(proposalRepository.findById(id)).thenReturn(Optional.of(proposal));

        // Act
        Optional<Proposal> result = proposalService.findById(id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void findByUser_ShouldReturnPageOfProposals() {
        // Arrange
        User user = User.builder().id(1L).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Proposal> page = new PageImpl<>(Collections.emptyList());
        when(proposalRepository.findBySubmittedBy(user, pageable)).thenReturn(page);

        // Act
        Page<Proposal> result = proposalService.findByUser(user, pageable);

        // Assert
        assertNotNull(result);
        verify(proposalRepository).findBySubmittedBy(user, pageable);
    }

    @Test
    void getStatsByUser_ShouldReturnStats() {
        // Arrange
        User user = User.builder().id(1L).build();
        ProposalStatsProjection stats = mock(ProposalStatsProjection.class);
        when(proposalRepository.getStatsByUserId(user.getId())).thenReturn(stats);

        // Act
        ProposalStatsProjection result = proposalService.getStatsByUser(user);

        // Assert
        assertNotNull(result);
        verify(proposalRepository).getStatsByUserId(user.getId());
    }

    @Test
    void delete_ShouldDeleteProposalAndEvictCache() {
        // Arrange
        Long id = 1L;
        User user = User.builder().id(10L).build();
        Proposal proposal = new MarkOccurrenceProposal();
        proposal.setId(id);
        proposal.setSubmittedBy(user);

        when(proposalRepository.findById(id)).thenReturn(Optional.of(proposal));
        when(cacheManager.getCache("proposalStats")).thenReturn(cache);

        // Act
        proposalService.delete(id);

        // Assert
        verify(proposalRepository).delete(proposal);
        verify(cache).evict(user.getId());
    }
}
