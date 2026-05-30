package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.user.dtos.UserDto;
import pt.estga.user.mappers.UserMapper;
import pt.estga.user.repositories.UserRepository;
import pt.estga.userapi.UserLookupOperations;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserLookupAdapter implements UserLookupOperations {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<UserDto> findById(Long id) {
        return userRepository.findById(id).map(userMapper::toDto);
    }
}
