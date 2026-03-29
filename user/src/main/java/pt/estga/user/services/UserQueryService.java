package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.sharedweb.filtering.QueryProcessor;
import pt.estga.sharedweb.models.PagedRequest;
import pt.estga.sharedweb.models.QueryResult;
import pt.estga.user.dtos.UserDto;
import pt.estga.user.entities.User;
import pt.estga.user.mappers.UserMapper;
import pt.estga.user.repositories.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository repository;
    private final QueryProcessor<User> queryProcessor;
    private final UserMapper mapper;

    public Page<UserDto> search(PagedRequest request) {
        QueryResult<User> result = queryProcessor.process(request);

        Page<User> userPage = repository.findAll(
                result.specification(),
                result.pageable()
        );

        return userPage.map(mapper::toDto);
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
}
