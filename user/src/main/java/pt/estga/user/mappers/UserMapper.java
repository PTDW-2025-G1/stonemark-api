package pt.estga.user.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pt.estga.user.dtos.ProfileUpdateRequestDto;
import pt.estga.user.dtos.UserPublicDto;
import pt.estga.user.dtos.UserDto;
import pt.estga.user.entities.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "photoId", source = "photo.id")
    UserDto toDto(User user);

    @Mapping(target = "photoId", source = "photo.id")
    UserPublicDto toPublicDto(User user);

    @Mapping(target = "photo", ignore = true)
    User toEntity(UserDto dto);

    @Mapping(target = "photo", ignore = true)
    void update(@MappingTarget User user, ProfileUpdateRequestDto dto);

}
