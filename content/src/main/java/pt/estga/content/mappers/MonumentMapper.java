package pt.estga.content.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pt.estga.content.dtos.*;
import pt.estga.content.dtos.MonumentDto;
import pt.estga.content.entities.Monument;
import pt.estga.file.mappers.MediaFileMapper;
import pt.estga.territory.mappers.AdministrativeDivisionMapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MediaFileMapper.class, AdministrativeDivisionMapper.class})
public interface MonumentMapper {

    MonumentDto toResponseDto(Monument monument);

    MonumentListDto toListDto(Monument monument);

    @Mapping(source = "parishId", target = "parish.id")
    @Mapping(source = "municipalityId", target = "municipality.id")
    @Mapping(source = "districtId", target = "district.id")
    @Mapping(target = "location", ignore = true)
    Monument toEntity(MonumentRequestDto dto);

    @Mapping(source = "parishId", target = "parish.id")
    @Mapping(source = "municipalityId", target = "municipality.id")
    @Mapping(source = "districtId", target = "district.id")
    @Mapping(target = "location", ignore = true)
    void updateEntityFromDto(MonumentRequestDto dto, @MappingTarget Monument entity);

}
