package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.shared.enums.UserRole;
import pt.estga.user.repositories.UserRepository;
import pt.estga.user.repositories.UserIdentityRepository;
import pt.estga.user.entities.User;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceHibernateImpl implements UserService {

    private final UserRepository repository;
    private final UserIdentityRepository userIdentityRepository;

    @Override
    public Page<User> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByPhone(String phone) {
        return repository.findByPhone(phone);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByKeycloakSub(String keycloakSub) {
        return repository.findByKeycloakSub(keycloakSub);
    }

    @Override
    public Optional<User> findByIdForProfile(Long id) {
        return repository.findByIdForProfile(id);
    }

    @Override
    public Optional<User> findByIdWithIdentities(Long id) {
        return repository.findByIdWithIdentities(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return repository.existsByPhone(phone);
    }

    @Override
    public boolean existsByKeycloakSub(String keycloakSub) {
        return repository.existsByKeycloakSub(keycloakSub);
    }

    @Override
    @Transactional
    public void deactivateByKeycloakSub(String keycloakSub) {
        User user = repository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new RuntimeException("User not found for keycloakSub"));

        user.setEnabled(false);
        user.setAccountLocked(true);
        repository.save(user);
    }

    @Override
    public User create(User user) {
        return repository.save(user);
    }

    @Override
    public User update(User user) {
        return repository.save(user);
    }

    @Override
    public Optional<User> updateRole(User user, UserRole role) {
        user.setRole(role);
        return Optional.ofNullable(update(user));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void softDeleteUser(Long id) {
        User user = repository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        userIdentityRepository.deleteByUser(user);

        user.setFirstName("deleted");
        user.setLastName("user");
        user.setUsername(null);
        user.setEmail(null);
        user.setEmailVerified(false);
        user.setEnabled(false);

        repository.save(user);
    }
}
