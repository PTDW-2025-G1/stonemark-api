package pt.estga.contact.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pt.estga.contact.entities.ContactRequest;
import pt.estga.contact.repositories.ContactRequestRepository;
import pt.estga.filterutils.QueryProcessor;
import pt.estga.filterutils.models.PagedRequest;
import pt.estga.filterutils.models.QueryResult;
import java.util.Optional;

/**
 * Read-only query service for contact requests. This keeps read operations
 * separate from mutation logic in ContactRequestService.
 */
@Service
@RequiredArgsConstructor
public class ContactRequestQueryService {

	private final ContactRequestRepository repository;
	private final QueryProcessor<ContactRequest> queryProcessor;

	public Page<ContactRequest> search(PagedRequest request) {
		QueryResult<ContactRequest> result = queryProcessor.process(request);

		return repository.findAll(
				result.specification(),
				result.pageable()
		);
	}

	public Page<ContactRequest> findAll(Pageable pageable) {
		return repository.findAll(pageable);
	}

	public Page<ContactRequest> findAllBySubmittedBy(Long submittedById, Pageable pageable) {
		return repository.findBySubmittedById(submittedById, pageable);
	}

	public Optional<ContactRequest> findById(Long id) {
		return repository.findById(id);
	}
}
