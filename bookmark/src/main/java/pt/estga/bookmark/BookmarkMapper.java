package pt.estga.bookmark;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookmarkMapper {

    @Mapping(source = "targetType", target = "type")
    @Mapping(target = "content", ignore = true)
    BookmarkDto toDto(Bookmark bookmark);

    @Mapping(source = "type", target = "targetType")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Bookmark toEntity(BookmarkDto dto);
}

