package pt.estga.mark.repositories;

import org.springframework.stereotype.Repository;
import pt.estga.mark.entities.Mark;
import pt.estga.shared.repositories.BaseRepository;

@Repository
public interface MarkRepository extends BaseRepository<Mark, Long> {
}
