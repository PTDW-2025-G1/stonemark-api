package pt.estga.proposal.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.projections.ProposalStatsProjection;
import pt.estga.proposal.repositories.ProposalRepository;
import pt.estga.user.entities.User;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposalServiceImpl implements ProposalService {

    private final ProposalRepository<Proposal> proposalRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    public Page<Proposal> getAll(Pageable pageable) {
        return proposalRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "proposals", key = "#id")
    public Optional<Proposal> findById(Long id) {
        return proposalRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Proposal> findByUser(User user, Pageable pageable) {
        return proposalRepository.findBySubmittedBy(user, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "proposalStats", key = "#user.id")
    public ProposalStatsProjection getStatsByUser(User user) {
        return proposalRepository.getStatsByUserId(user.getId());
    }

    @Override
    @Transactional
    @CacheEvict(value = "proposals", key = "#id")
    public void delete(Long id) {
        proposalRepository.findById(id).ifPresent(proposal -> {
            // Also evict stats for the user who submitted the proposal
            if (proposal.getSubmittedBy() != null) {
                Optional.ofNullable(cacheManager.getCache("proposalStats"))
                        .ifPresent(cache -> cache.evict(proposal.getSubmittedBy().getId()));
            }
            proposalRepository.delete(proposal);
        });
    }
}
