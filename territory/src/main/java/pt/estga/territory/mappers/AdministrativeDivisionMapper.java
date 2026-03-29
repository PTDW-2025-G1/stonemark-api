package pt.estga.territory.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pt.estga.territory.dtos.AdministrativeDivisionDto;
import pt.estga.territory.dtos.AdministrativeDivisionRequestDto;
import pt.estga.territory.entities.AdministrativeDivision;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdministrativeDivisionMapper {

    @Mapping(target = "parentId", source = "parent.id")
    AdministrativeDivisionDto toDto(AdministrativeDivision entity);

    List<AdministrativeDivisionDto> toDtoList(List<AdministrativeDivision> entities);

    void update(AdministrativeDivision source, @MappingTarget AdministrativeDivision target);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "geometry", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "country", ignore = true)
    AdministrativeDivision toEntityFromRequest(AdministrativeDivisionRequestDto dto);

    @org.mapstruct.BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "geometry", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "country", ignore = true)
    void updateFromRequest(AdministrativeDivisionRequestDto dto, @MappingTarget AdministrativeDivision entity);
}
