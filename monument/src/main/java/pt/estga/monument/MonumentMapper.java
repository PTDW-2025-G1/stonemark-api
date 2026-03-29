package pt.estga.monument;

import org.mapstruct.*;
import pt.estga.monument.dots.MonumentDto;
import pt.estga.monument.dots.MonumentListDto;
import pt.estga.monument.dots.MonumentRequestDto;
import pt.estga.territory.mappers.AdministrativeDivisionMapper;
import org.locationtech.jts.geom.Point;
import pt.estga.territory.utils.GeometryUtils;

@Mapper(
        componentModel = "spring",
        uses = {AdministrativeDivisionMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MonumentMapper {

    @Mapping(target = "latitude", source = "location.y")
    @Mapping(target = "longitude", source = "location.x")
    MonumentDto toResponseDto(Monument monument);

    MonumentListDto toListDto(Monument monument);

    @Mapping(target = "location", expression = "java(toPoint(dto))")
    @Mapping(target = "division", ignore = true)
    Monument toEntity(MonumentRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "location", expression = "java(toPoint(dto))")
    @Mapping(target = "division", ignore = true)
    void updateEntityFromDto(MonumentRequestDto dto, @MappingTarget Monument entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "division", ignore = true)
    void update(Monument source, @MappingTarget Monument target);

    default Point toPoint(MonumentRequestDto dto) {
        return GeometryUtils.createPoint(dto.latitude(), dto.longitude());
    }

    @AfterMapping
    default void linkDivision(MonumentRequestDto dto, @MappingTarget Monument entity) {
        if (dto.divisionId() != null) {
            // This assumes your Service will ensure the division exists
            // or you inject a Repository here (not recommended for Mappers)
        }
    }
}