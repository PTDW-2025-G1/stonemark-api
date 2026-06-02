package pt.estga.userapi;

import pt.estga.user.dtos.UserDto;

import java.util.Optional;

public interface UserLookupOperations {

    Optional<UserDto> findById(Long id);
}
