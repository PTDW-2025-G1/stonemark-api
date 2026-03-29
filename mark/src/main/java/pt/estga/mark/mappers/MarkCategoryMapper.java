package pt.estga.mark.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pt.estga.mark.dtos.MarkCategoryRequestDto;
import pt.estga.mark.entities.MarkCategory;

@Mapper(componentModel = "spring")
public interface MarkCategoryMapper {

    @Mapping(target = "id", ignore = true)
    MarkCategory toEntity(MarkCategoryRequestDto dto);

    void update(MarkCategory source, @MappingTarget MarkCategory target);

    @Mapping(target = "parentCategory", ignore = true)
    void updateFromDto(MarkCategoryRequestDto dto, @MappingTarget MarkCategory entity);

}
