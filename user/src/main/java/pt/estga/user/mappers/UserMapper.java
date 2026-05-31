package pt.estga.user.mappers;

import org.springframework.stereotype.Component;
import pt.estga.user.dtos.ProfileUpdateRequestDto;
import pt.estga.user.dtos.UserPublicDto;
import pt.estga.user.dtos.UserDto;
import pt.estga.user.entities.Role;
import pt.estga.user.entities.User;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) return null;
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return new UserDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getEmail(),
                null,
                roleNames,
                user.getCreatedAt()
        );
    }

    public UserPublicDto toPublicDto(User user) {
        if (user == null) return null;
        return new UserPublicDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                null
        );
    }

    public void update(User source, User target) {
        if (source == null || target == null) return;
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setUsername(source.getUsername());
        target.setEmail(source.getEmail());
    }

    public void update(User user, ProfileUpdateRequestDto dto) {
        if (user == null || dto == null) return;
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
    }

    public void updateFromDto(UserDto dto, User user) {
        if (dto == null || user == null) return;
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setUsername(dto.username());
        user.setEmail(dto.email());
    }
}
