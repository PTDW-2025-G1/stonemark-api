package pt.estga.shared.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import pt.estga.shared.entities.BaseEntity;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {

    default void softDelete(T entity) {
        if (entity instanceof BaseEntity base) {
            base.markDeleted();
        }
    }
}