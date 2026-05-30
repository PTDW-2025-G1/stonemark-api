package pt.estga.user.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pt.estga.user.dtos.ProfileUpdateRequestDto;
import pt.estga.user.dtos.UserPublicDto;
import pt.estga.user.dtos.UserDto;
import pt.estga.user.entities.Role;
import pt.estga.user.entities.User;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    UserPublicDto toPublicDto(User user);

    @Mapping(target = "roles", ignore = true)
    User toEntity(UserDto dto);

    void update(@MappingTarget User user, ProfileUpdateRequestDto dto);

    @Mapping(target = "roles", ignore = true)
    void updateFromDto(UserDto dto, @MappingTarget User user);

    void update(User source, @MappingTarget User target);

    default Set<String> rolesToStringSet(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }

}
