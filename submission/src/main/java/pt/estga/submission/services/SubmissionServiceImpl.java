package pt.estga.submission.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.projections.ProposalStatsProjection;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;
import pt.estga.user.entities.User;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final MarkOccurrenceSubmissionRepository markOccurrenceSubmissionRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    public Page<MarkOccurrenceSubmission> getAll(Pageable pageable) {
        return markOccurrenceSubmissionRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "proposals", key = "#id")
    public Optional<MarkOccurrenceSubmission> findById(Long id) {
        return markOccurrenceSubmissionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MarkOccurrenceSubmission> findByUser(User user, Pageable pageable) {
        return markOccurrenceSubmissionRepository.findBySubmittedBy(user, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "proposalStats", key = "#user.id")
    public ProposalStatsProjection getStatsByUser(User user) {
        return markOccurrenceSubmissionRepository.getStatsByUserId(user.getId());
    }

    @Override
    @Transactional
    @CacheEvict(value = "proposals", key = "#id")
    public void delete(Long id) {
        markOccurrenceSubmissionRepository.findById(id).ifPresent(proposal -> {
            // Also evict stats for the user who submitted the proposal
            if (proposal.getSubmittedBy() != null) {
                Optional.ofNullable(cacheManager.getCache("proposalStats"))
                        .ifPresent(cache -> cache.evict(proposal.getSubmittedBy().getId()));
            }
            markOccurrenceSubmissionRepository.delete(proposal);
        });
    }
}
