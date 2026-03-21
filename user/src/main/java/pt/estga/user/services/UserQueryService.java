package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import pt.estga.filterutils.QueryProcessor;
import pt.estga.filterutils.models.PagedRequest;
import pt.estga.filterutils.models.QueryResult;
import pt.estga.filterutils.SpecificationBuilder;
import pt.estga.user.dtos.UserDto;
import pt.estga.user.entities.User;
import pt.estga.user.mappers.UserMapper;
import pt.estga.user.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;
    private final SpecificationBuilder<User> specificationBuilder;
    private final UserMapper mapper;

    public Page<UserDto> search(PagedRequest request) {
        QueryProcessor<User> processor = new QueryProcessor<>(specificationBuilder);
        QueryResult<User> result = processor.process(request);
        Page<User> page = (result.specification() == null)
                ? userRepository.findAll(result.pageable())
                : userRepository.findAll(result.specification(), result.pageable());
        return page.map(mapper::toDto);
    }
}
