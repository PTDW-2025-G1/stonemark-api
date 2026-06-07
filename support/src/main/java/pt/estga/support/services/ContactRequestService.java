package pt.estga.support.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.sharedinfra.jpa.SpecBuilder;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;
import pt.estga.support.dtos.ContactRequestDto;
import pt.estga.support.dtos.ContactRequestFilter;
import pt.estga.support.entities.ContactRequest;
import pt.estga.support.enums.ContactStatus;
import pt.estga.support.mappers.ContactRequestMapper;
import pt.estga.support.repositories.ContactRequestRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactRequestService {

    private final ContactRequestRepository repository;

    @Transactional
    public ContactRequest create(ContactRequestDto dto) {
        ContactRequest contact = ContactRequestMapper.toEntity(dto);
        contact.setStatus(ContactStatus.PENDING);
        contact.setCreatedAt(Instant.now());
        return repository.save(contact);
    }

    public Optional<ContactRequest> findById(Long id) {
        return repository.findById(id);
    }

    public Page<ContactRequest> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<ContactRequest> search(ContactRequestFilter filter, Pageable pageable) {
        var sb = new SpecBuilder<ContactRequest>()
                .eq("status", filter.status())
                .like("name", filter.name())
                .like("email", filter.email());
        return repository.findAll(sb.build(), pageable);
    }

    @Transactional
    public ContactRequest updateStatus(Long id, ContactStatus status) {
        ContactRequest contact = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact request not found with id: " + id));
        contact.setStatus(status);
        return repository.save(contact);
    }

    @Transactional
    public void delete(Long id) {
        ContactRequest contact = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact request not found with id: " + id));
        repository.softDelete(contact);
    }
}
