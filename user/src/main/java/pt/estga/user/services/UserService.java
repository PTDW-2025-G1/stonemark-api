package pt.estga.user.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.estga.shared.enums.UserRole;
import pt.estga.user.entities.User;

import java.time.Instant;
import java.util.Optional;

public interface UserService {

    Page<User> findAll(Pageable pageable);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByKeycloakSub(String keycloakSub);

    Optional<User> findByIdForProfile(Long id);

    Optional<User> findByIdWithIdentities(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByKeycloakSub(String keycloakSub);

    void deactivateByKeycloakSub(String keycloakSub);

    User create(User user);

    User update(User user);

    Optional<User> updateRole(User user, UserRole role);

    void deleteById(Long id);

    void softDeleteUser(Long id);
}
