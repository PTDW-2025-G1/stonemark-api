package pt.estga.contact.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.contact.repositories.ContactRequestRepository;
import pt.estga.contact.enums.ContactStatus;
import pt.estga.contact.dtos.ContactRequestDto;
import pt.estga.contact.entities.ContactRequest;
import pt.estga.shared.exceptions.ContactNotFoundException;
import pt.estga.user.entities.User;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ContactRequestService {

    private final ContactRequestRepository repository;

    public ContactRequest create(ContactRequestDto dto) {
        return create(dto, null);
    }

    public ContactRequest create(ContactRequestDto dto, Long submittedById) {
        ContactRequest contact = new ContactRequest();

        contact.setName(dto.name());
        contact.setEmail(dto.email());
        contact.setSubject(dto.subject());
        contact.setMessage(dto.message());
        contact.setStatus(ContactStatus.PENDING);
        contact.setCreatedAt(Instant.now());
        contact.setSubmittedBy(User.builder().id(submittedById).build());

        return repository.save(contact);
    }


    public ContactRequest updateStatus(Long id, ContactStatus status) {
        ContactRequest contact = repository.findById(id)
                .orElseThrow(() -> new ContactNotFoundException(id));

        contact.setStatus(status);
        return repository.save(contact);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ContactNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
