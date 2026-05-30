package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;
import pt.estga.sharedweb.filtering.QueryProcessor;
import pt.estga.sharedweb.models.PagedRequest;
import pt.estga.sharedweb.models.QueryResult;
import pt.estga.user.dtos.UserDto;
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
    private final QueryProcessor<User> queryProcessor;
    private final UserMapper mapper;
    private final AuthenticationService authenticationService;

    public Page<UserDto> search(PagedRequest request) {
        QueryResult<User> result = queryProcessor.process(request);
        Page<User> userPage = repository.findAll(result.specification(), result.pageable());
        return userPage.map(mapper::toDto);
    }

    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<UserDto> findDtoById(Long id) {
        return repository.findById(id).map(mapper::toDto);
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
        mapper.update(user, existing);
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
        mapper.updateFromDto(dto, existing);
        User saved = repository.save(existing);
        authenticationService.invalidateCache(saved.getId());
        return mapper.toDto(saved);
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
}
