package pt.estga.mark.repositories;

import org.springframework.stereotype.Repository;
import pt.estga.mark.entities.Mark;
import pt.estga.shared.core.enums.ValidationState;
import pt.estga.shared.jpa.repositories.BaseRepository;

import java.util.List;

@Repository
public interface MarkRepository extends BaseRepository<Mark, Long> {
	List<Mark> findByValidationState(ValidationState state);
}
