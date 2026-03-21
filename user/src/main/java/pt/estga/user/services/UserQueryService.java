package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import pt.estga.filterutils.QueryProcessor;
import pt.estga.filterutils.models.PagedRequest;
import pt.estga.filterutils.models.QueryResult;
import pt.estga.user.dtos.UserDto;
import pt.estga.user.entities.User;
import pt.estga.user.mappers.UserMapper;
import pt.estga.user.repositories.UserRepository;

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
}
