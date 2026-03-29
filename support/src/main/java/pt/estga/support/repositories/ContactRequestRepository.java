package pt.estga.support.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pt.estga.support.entities.ContactRequest;
import pt.estga.shared.repositories.BaseRepository;

public interface ContactRequestRepository extends BaseRepository<ContactRequest, Long>, JpaSpecificationExecutor<ContactRequest> {
}
