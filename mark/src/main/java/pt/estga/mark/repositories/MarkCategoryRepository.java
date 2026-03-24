package pt.estga.mark.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.mark.entities.MarkCategory;

public interface MarkCategoryRepository extends JpaRepository<MarkCategory, Long> {
}
