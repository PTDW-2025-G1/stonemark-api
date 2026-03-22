package pt.estga.monument;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pt.estga.monument.dots.MonumentDto;
import pt.estga.monument.dots.MonumentListDto;
import pt.estga.monument.dots.MonumentRequestDto;
import pt.estga.territory.mappers.AdministrativeDivisionMapper;

@Mapper(componentModel = "spring", uses = {AdministrativeDivisionMapper.class})
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
