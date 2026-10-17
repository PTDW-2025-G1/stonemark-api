package pt.estga.shared.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import pt.estga.shared.jpa.entities.SoftDeletable;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {

    default void softDelete(T entity) {
        if (entity instanceof SoftDeletable softDeletable) {
            softDeletable.markDeleted();
        }
    }
}