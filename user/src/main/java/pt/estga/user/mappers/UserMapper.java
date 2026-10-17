package pt.estga.user.mappers;

import pt.estga.user.dtos.ProfileUpdateRequestDto;
import pt.estga.user.dtos.UserPublicDto;
import pt.estga.user.dtos.UserDto;
import pt.estga.user.entities.User;

public class UserMapper {

    private UserMapper() {}

    public static UserDto toDto(User user) {
        if (user == null) return null;
        return new UserDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getEmail(),
                null,
                user.getRole(),
                user.getCreatedAt()
        );
    }

    public static UserPublicDto toPublicDto(User user) {
        if (user == null) return null;
        return new UserPublicDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                null
        );
    }

    public static void update(User source, User target) {
        if (source == null || target == null) return;
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setUsername(source.getUsername());
        target.setEmail(source.getEmail());
    }

    public static void update(User user, ProfileUpdateRequestDto dto) {
        if (user == null || dto == null) return;
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
    }

    public static void updateFromDto(UserDto dto, User user) {
        if (dto == null || user == null) return;
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        if (dto.role() != null) {
            user.setRole(dto.role());
        }
    }
}
