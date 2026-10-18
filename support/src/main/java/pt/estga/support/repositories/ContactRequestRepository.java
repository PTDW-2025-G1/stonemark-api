package pt.estga.support.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pt.estga.support.entities.ContactRequest;
import pt.estga.shared.jpa.repositories.BaseRepository;

public interface ContactRequestRepository extends BaseRepository<ContactRequest, UUID>, JpaSpecificationExecutor<ContactRequest> {
}
