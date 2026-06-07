package pt.estga.user.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.sharedinfra.repositories.BaseRepository;
import pt.estga.user.entities.User;

import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByGoogleSub(String googleSub);

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForProfile(@Param("id") Long id);

    boolean existsByUsername(String username);

    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.id = :id")
    Optional<User> findByIdWithRolesAndPermissions(@Param("id") Long id);

}
