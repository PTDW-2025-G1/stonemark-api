package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.user.repositories.UserRepository;
import pt.estga.user.repositories.ChatbotAccountRepository;
import pt.estga.user.entities.User;
import pt.estga.sharedweb.filtering.QueryProcessor;
import pt.estga.sharedweb.models.PagedRequest;
import pt.estga.sharedweb.models.QueryResult;
import pt.estga.sharedweb.utils.FilterValidator;
import pt.estga.user.dtos.UserPublicDto;
import pt.estga.user.mappers.UserMapper;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final ChatbotAccountRepository chatbotAccountRepository;
    private final QueryProcessor<User> queryProcessor;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    public Page<UserPublicDto> searchPublicUsers(PagedRequest request) {
        FilterValidator.validate(request.getFilter(), UserPublicDto.class);
        FilterValidator.validateSort(request.getSort(), UserPublicDto.class);

        QueryResult<User> result = queryProcessor.process(request);

        Page<User> page = repository.findAll(
                result.specification(),
                result.pageable()
        );

        return page.map(mapper::toPublicDto);
    }

    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByKeycloakSub(String keycloakSub) {
        return repository.findByKeycloakSub(keycloakSub);
    }

    public Optional<User> findByIdForProfile(Long id) {
        return repository.findByIdForProfile(id);
    }

    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    public User create(User user) {
        return repository.save(user);
    }

    public User update(User user) {
        return repository.save(user);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public void softDeleteUser(Long id) {
        User user = repository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        chatbotAccountRepository.deleteByUser(user);

        user.setFirstName("deleted");
        user.setLastName("user");
        user.setUsername(null);
        user.setEmail(null);
        user.setEmailVerified(false);
        user.setEnabled(false);

        repository.save(user);
    }
}
