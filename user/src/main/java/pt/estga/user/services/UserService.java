package pt.estga.user.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.estga.shared.enums.UserRole;
import pt.estga.user.entities.User;

import java.util.Optional;

public interface UserService {

    Page<User> findAll(Pageable pageable);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByKeycloakSub(String keycloakSub);

    Optional<User> findByIdForProfile(Long id);

    boolean existsByUsername(String username);

    User create(User user);

    User update(User user);

    Optional<User> updateRole(User user, UserRole role);

    void deleteById(Long id);

    void softDeleteUser(Long id);
}
