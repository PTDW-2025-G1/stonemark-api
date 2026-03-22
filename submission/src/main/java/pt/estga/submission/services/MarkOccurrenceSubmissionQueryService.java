package pt.estga.submission.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pt.estga.sharedweb.filtering.QueryProcessor;
import pt.estga.sharedweb.models.PagedRequest;
import pt.estga.sharedweb.models.QueryResult;
import pt.estga.submission.dtos.ProposalAdminListDto;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.mappers.SubmissionAdminMapper;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;

import java.util.Optional;

/**
 * Read-only query service for mark occurrence submissions. Encapsulates repository
 * queries and small normalization logic used by controllers.
 */
@Service
@RequiredArgsConstructor
public class MarkOccurrenceSubmissionQueryService {

	private final MarkOccurrenceSubmissionRepository repository;
	private final QueryProcessor<MarkOccurrenceSubmission> queryProcessor;
	private final SubmissionAdminMapper submissionAdminMapper;

	public Page<ProposalAdminListDto> search(PagedRequest request) {
		QueryResult<MarkOccurrenceSubmission> result = queryProcessor.process(request);

		Page<MarkOccurrenceSubmission> entityPage = repository.findAll(
				result.specification(),
				result.pageable()
		);

		return entityPage.map(submissionAdminMapper::toAdminListDto);
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


