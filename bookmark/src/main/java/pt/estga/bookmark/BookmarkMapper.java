package pt.estga.bookmark;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.estga.bookmark.entities.BaseBookmark;

@Mapper(componentModel = "spring")
public interface BookmarkMapper {

    @Mapping(source = "targetType", target = "type")
    @Mapping(target = "content", ignore = true)
    BookmarkDto toDto(BaseBookmark bookmark);

    @Mapping(source = "type", target = "targetType")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    BaseBookmark toEntity(BookmarkDto dto);
}

