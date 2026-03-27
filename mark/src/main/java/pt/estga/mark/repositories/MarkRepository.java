package pt.estga.mark.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.estga.mark.entities.Mark;

@Repository
public interface MarkRepository extends JpaRepository<Mark, Long> {
}
