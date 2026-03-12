package contact;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import pt.estga.contact.enums.ContactStatus;
import pt.estga.contact.dtos.ContactRequestDto;
import pt.estga.contact.entities.ContactRequest;
import pt.estga.contact.repositories.ContactRequestRepository;
import pt.estga.contact.services.ContactRequestServiceHibernateImpl;
import pt.estga.shared.exceptions.ContactNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactRequestServiceHibernateImplTest {

    @Mock
    private ContactRequestRepository repository;

    @InjectMocks
    private ContactRequestServiceHibernateImpl service;

    private ContactRequestDto requestDto;
    private ContactRequest testContact;

    @BeforeEach
    void setUp() {
        requestDto = new ContactRequestDto(
                "John Doe",
                "john@example.com",
                "general",
                "Hello test message!"
        );

        testContact = ContactRequest.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .subject("general")
                .message("Hello test message!")
                .status(ContactStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should create a new contact message")
    void testCreateContact() {
        // Given
        when(repository.save(any(ContactRequest.class))).thenReturn(testContact);

        // When
        ContactRequest saved = service.create(requestDto);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo("John Doe");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");

        ArgumentCaptor<ContactRequest> captor = ArgumentCaptor.forClass(ContactRequest.class);
        verify(repository).save(captor.capture());

        ContactRequest captured = captor.getValue();

        assertThat(captured.getStatus()).isEqualTo(ContactStatus.PENDING);
        assertThat(captured.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return all contact messages")
    void testFindAll() {
        Pageable pageable =
                PageRequest.of(0, 10);
        Page<ContactRequest> page = new PageImpl<>(List.of(testContact), pageable, 1);

        when(repository.findAll(pageable)).thenReturn(page);

        Page<ContactRequest> result = service.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(1L);

        verify(repository).findAll(pageable);
    }

    @Test
    @DisplayName("Should return a contact message by ID")
    void testFindById() {
        when(repository.findById(1L)).thenReturn(Optional.of(testContact));

        Optional<ContactRequest> found = service.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should update contact status")
    void testUpdateStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(testContact));
        when(repository.save(any(ContactRequest.class))).thenReturn(testContact);

        ContactRequest updated = service.updateStatus(1L, ContactStatus.RESOLVED);

        assertThat(updated.getStatus()).isEqualTo(ContactStatus.RESOLVED);
    }

    @Test
    @DisplayName("Should throw ContactNotFoundException when updating status for non-existing message")
    void testUpdateStatus_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ContactNotFoundException.class)
                .isThrownBy(() -> service.updateStatus(99L, ContactStatus.PENDING));
    }

    @Test
    @DisplayName("Should delete a contact message")
    void testDelete() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw ContactNotFoundException when deleting non-existing message")
    void testDelete_notFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatExceptionOfType(ContactNotFoundException.class)
                .isThrownBy(() -> service.delete(99L));
    }
}
