package pt.estga.support.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.support.repositories.ContactRequestRepository;
import pt.estga.support.enums.ContactStatus;
import pt.estga.support.dtos.ContactRequestDto;
import pt.estga.support.entities.ContactRequest;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ContactRequestService {

    private final ContactRequestRepository repository;

    public ContactRequest create(ContactRequestDto dto) {
        ContactRequest contact = new ContactRequest();

        contact.setName(dto.name());
        contact.setEmail(dto.email());
        contact.setSubject(dto.subject());
        contact.setMessage(dto.message());
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
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Contact request not found with id: " + id);
        }
        repository.deleteById(id);
    }
}
