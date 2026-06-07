package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.commoninfra.jpa.SpecBuilder;
import pt.estga.commonweb.exceptions.ResourceNotFoundException;
import pt.estga.user.dtos.MeDto;
import pt.estga.user.dtos.ProfileUpdateRequestDto;
import pt.estga.user.dtos.UserDto;
import pt.estga.user.dtos.UserFilter;
import pt.estga.user.entities.User;
import pt.estga.user.mappers.UserMapper;
import pt.estga.user.repositories.ChatbotAccountRepository;
import pt.estga.user.repositories.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;
    private final ChatbotAccountRepository chatbotAccountRepository;
    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;

    public Page<UserDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(UserMapper::toDto);
    }

    public Page<UserDto> search(UserFilter filter, Pageable pageable) {
        var sb = new SpecBuilder<User>()
                .like("username", filter.username())
                .like("email", filter.email())
                .isTrue("enabled", filter.enabled());
        return repository.findAll(sb.build(), pageable).map(UserMapper::toDto);
    }

    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<UserDto> findDtoById(Long id) {
        return repository.findById(id).map(UserMapper::toDto);
    }

    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public Optional<User> findByIdForProfile(Long id) {
        return repository.findByIdForProfile(id);
    }

    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Transactional
    public User create(User user) {
        return repository.save(user);
    }

    @Transactional
    public User update(User user) {
        if (user.getId() == null) {
            throw new ResourceNotFoundException("User id must not be null for update");
        }
        User existing = repository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + user.getId() + " not found"));
        UserMapper.update(user, existing);
        User saved = repository.save(existing);
        authenticationService.invalidateCache(saved.getId());
        return saved;
    }

    @Transactional
    public UserDto updateFromDto(Long id, UserDto dto) {
        if (id == null) {
            throw new IllegalArgumentException("Id must be provided");
        }
        User existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        UserMapper.updateFromDto(dto, existing);
        User saved = repository.save(existing);
        authenticationService.invalidateCache(saved.getId());
        return UserMapper.toDto(saved);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void softDeleteUser(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        chatbotAccountRepository.deleteByUser(user);
        repository.softDelete(user);
    }

    public UserDto getProfile(Long userId) {
        User user = repository.findByIdForProfile(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return UserMapper.toDto(user);
    }

    public MeDto getMe(Long userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return new MeDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPasswordHash() != null,
                user.isEnabled(),
                user.isAccountLocked()
        );
    }

    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequestDto request) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        UserMapper.update(user, request);
        repository.save(user);
        authenticationService.invalidateCache(user.getId());
    }

    @Transactional
    public void setPassword(Long userId, String rawPassword) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (user.getPasswordHash() != null) {
            throw new IllegalArgumentException("Password is already set. Use PUT to change it.");
        }
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        repository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (user.getPasswordHash() == null) {
            throw new IllegalArgumentException("No password set. Use POST to set one first.");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        repository.save(user);
    }
}
