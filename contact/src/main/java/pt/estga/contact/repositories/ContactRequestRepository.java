package pt.estga.contact.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.contact.entities.ContactRequest;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, Long> {
    Page<ContactRequest> findBySubmittedById(Long submittedById, Pageable pageable);
}
