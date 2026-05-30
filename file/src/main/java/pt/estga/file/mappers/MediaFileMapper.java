package pt.estga.file.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.estga.file.dtos.MediaFileDto;
import pt.estga.file.entities.MediaFile;

@Mapper(componentModel = "spring")
public interface MediaFileMapper {

    @Mapping(target = "url", ignore = true)
    MediaFileDto toDto(MediaFile entity);
}
