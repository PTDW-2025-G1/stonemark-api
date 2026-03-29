package pt.estga.user.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.shared.repositories.BaseRepository;
import pt.estga.user.entities.User;

import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByKeycloakSub(String keycloakSub);

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForProfile(@Param("id") Long id);

    boolean existsByUsername(String username);

}
