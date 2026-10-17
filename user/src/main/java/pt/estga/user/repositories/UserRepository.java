package pt.estga.user.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.shared.jpa.repositories.BaseRepository;
import pt.estga.user.entities.User;

import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForProfile(@Param("id") UUID id);

    boolean existsByUsername(String username);

}
