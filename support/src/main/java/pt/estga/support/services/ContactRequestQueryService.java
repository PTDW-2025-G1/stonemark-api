package pt.estga.support.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import pt.estga.support.entities.ContactRequest;
import pt.estga.support.repositories.ContactRequestRepository;
import pt.estga.sharedweb.filtering.QueryProcessor;
import pt.estga.sharedweb.models.PagedRequest;
import pt.estga.sharedweb.models.QueryResult;
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

	public Optional<ContactRequest> findById(Long id) {
		return repository.findById(id);
	}
}
