package pt.estga.processing.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.processing.entities.ProcessingOutbox;
import pt.estga.processing.enums.ProcessingStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessingOutboxRepository extends JpaRepository<ProcessingOutbox, UUID> {

    List<ProcessingOutbox> findByStatusOrderByCreatedAtAsc(ProcessingStatus status, Pageable pageable);

    Optional<ProcessingOutbox> findBySubmissionId(Long submissionId);

    void deleteBySubmissionId(Long submissionId);

}
