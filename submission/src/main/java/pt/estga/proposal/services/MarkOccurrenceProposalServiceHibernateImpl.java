package pt.estga.proposal.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.repositories.MarkOccurrenceProposalRepository;
import pt.estga.user.entities.User;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkOccurrenceProposalServiceHibernateImpl implements MarkOccurrenceProposalService {

    private final MarkOccurrenceProposalRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<MarkOccurrenceProposal> findByUser(User user, Pageable pageable) {
        return repository.findBySubmittedBy(user, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "proposals", key = "#id")
    public Optional<MarkOccurrenceProposal> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarkOccurrenceProposal> findByIdWithRelations(Long id) {
        return repository.findByIdWithRelations(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "proposalStats", key = "#proposal.submittedBy?.id")
    public MarkOccurrenceProposal create(MarkOccurrenceProposal proposal) {
        return repository.save(proposal);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "proposals", key = "#proposal.id"),
            @CacheEvict(value = "proposalStats", key = "#proposal.submittedBy?.id")
    })
    public MarkOccurrenceProposal update(MarkOccurrenceProposal proposal) {
        return repository.save(proposal);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "proposals", key = "#proposal.id"),
            @CacheEvict(value = "proposalStats", key = "#proposal.submittedBy?.id")
    })
    public void delete(MarkOccurrenceProposal proposal) {
        repository.delete(proposal);
    }
}
