package pt.estga.submission.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pt.estga.submission.dtos.ProposalFilter;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;

import java.util.Collection;
import java.util.Optional;

/**
 * Read-only query service for mark occurrence submissions. Encapsulates repository
 * queries and small normalization logic used by controllers.
 */
@Service
@RequiredArgsConstructor
public class MarkOccurrenceSubmissionQueryService {

	private final MarkOccurrenceSubmissionRepository repository;

	public Page<MarkOccurrenceSubmission> search(ProposalFilter filter, Pageable pageable) {
		Collection<SubmissionStatus> statuses = filter == null ? null : filter.statuses();
		if (statuses != null && statuses.isEmpty()) {
			statuses = null;
		}

		Long submittedById = filter == null ? null : filter.submittedById();
		return repository.findByFilters(statuses, submittedById, pageable);
	}

	public Page<MarkOccurrenceSubmission> findBySubmittedBy(Pageable pageable, pt.estga.user.entities.User user) {
		return repository.findBySubmittedBy(user, pageable);
	}

	public Optional<MarkOccurrenceSubmission> findByIdWithRelations(Long id) {
		return repository.findByIdWithRelations(id);
	}

	public Optional<MarkOccurrenceSubmission> findById(Long id) {
		return repository.findById(id);
	}

	public long countAcceptedByUserId(Long userId) {
		return repository.countAcceptedByUserId(userId);
	}
}


