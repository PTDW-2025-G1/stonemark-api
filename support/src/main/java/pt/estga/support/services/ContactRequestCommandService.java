package pt.estga.support.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.support.repositories.ContactRequestRepository;
import pt.estga.support.mappers.ContactRequestMapper;
import pt.estga.support.enums.ContactStatus;
import pt.estga.support.dtos.ContactRequestDto;
import pt.estga.support.entities.ContactRequest;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class ContactRequestCommandService {

    private final ContactRequestRepository repository;
    private final ContactRequestMapper mapper;

    public ContactRequest create(ContactRequestDto dto) {
        ContactRequest contact = mapper.toEntity(dto);
        contact.setStatus(ContactStatus.PENDING);
        contact.setCreatedAt(Instant.now());

        return repository.save(contact);
    }

    public ContactRequest updateStatus(Long id, ContactStatus status) {
        ContactRequest contact = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact request not found with id: " + id));

        contact.setStatus(status);
        return repository.save(contact);
    }

    public void delete(Long id) {
        ContactRequest contact = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact request not found with id: " + id));

        repository.softDelete(contact);
    }
}
