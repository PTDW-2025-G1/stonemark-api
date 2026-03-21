package pt.estga.contact.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pt.estga.contact.entities.ContactRequest;
import pt.estga.contact.repositories.ContactRequestRepository;

import java.util.Optional;

/**
 * Read-only query service for contact requests. This keeps read operations
 * separate from mutation logic in ContactRequestService.
 */
@Service
@RequiredArgsConstructor
public class ContactRequestQueryService {

	private final ContactRequestRepository repository;

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


