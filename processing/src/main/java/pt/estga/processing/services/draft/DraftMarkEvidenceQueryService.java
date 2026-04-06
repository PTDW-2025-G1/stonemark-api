package pt.estga.processing.services.draft;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.processing.repositories.DraftMarkEvidenceRepository;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.processing.enums.ProcessingStatus;

import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DraftMarkEvidenceQueryService {

    private final DraftMarkEvidenceRepository repository;
    private final Clock clock;
    @Value("${processing.enrichment.stale-timeout-minutes:10}")
    private long staleTimeoutMinutes;

    public Optional<DraftMarkEvidence> findBySubmissionId(Long submissionId) {
        return Optional.ofNullable(repository.findBySubmissionId(submissionId));
    }

    public Optional<DraftMarkEvidence> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Return IDs of drafts that are ready for processing.
     * Criteria: processingStatus = QUEUED OR processingStatus = IN_PROGRESS but lastModifiedAt older than staleTimeoutMinutes.
     */
    public List<Long> findSubmissionsReadyForProcessing(Pageable pageable) {
        Instant cutoff = Instant.now(clock).minus(Duration.ofMinutes(staleTimeoutMinutes));
        List<Long> draftIds = repository.findDraftIdsReadyForProcessing(pageable);
        if (log.isDebugEnabled()) log.debug("Repository returned {} candidate draft ids: {}", draftIds == null ? 0 : draftIds.size(), draftIds);

        List<Long> filtered = draftIds.stream()
                .filter(draftId -> repository.findById(draftId).map(d -> {
                    if (d.getProcessingStatus() == ProcessingStatus.QUEUED) return true;
                    if (d.getProcessingStatus() == ProcessingStatus.IN_PROGRESS)
                        return d.getLastModifiedAt() == null || d.getLastModifiedAt().isBefore(cutoff);
                    return false;
                }).orElse(false))
                .collect(Collectors.toList());

        if (log.isDebugEnabled()) log.debug("After filtering by status/cutoff ({}), returning {} draft ids: {}", staleTimeoutMinutes, filtered.size(), filtered);
        return filtered;
    }

    /**
     * Load draft with a pessimistic write lock to ensure safe concurrent review processing.
     */
    public Optional<DraftMarkEvidence> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id);
    }

    /**
     * Check whether the draft for the given submission id is ready for review.
     */
    public boolean isDraftReadyForReview(Long submissionId) {
        return findBySubmissionId(submissionId)
                .map(DraftMarkEvidence::isReadyForReview)
                .orElse(false);
    }
}
