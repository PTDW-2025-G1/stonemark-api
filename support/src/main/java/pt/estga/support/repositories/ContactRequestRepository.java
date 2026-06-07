package pt.estga.support.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pt.estga.support.entities.ContactRequest;
import pt.estga.sharedinfra.repositories.BaseRepository;

public interface ContactRequestRepository extends BaseRepository<ContactRequest, Long>, JpaSpecificationExecutor<ContactRequest> {
}
