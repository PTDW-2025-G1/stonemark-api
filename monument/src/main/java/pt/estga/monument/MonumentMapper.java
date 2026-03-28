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
public abstract class MonumentMapper {

    // 1. Entity -> DTO (Response)
    @Mapping(target = "latitude", source = "location.y")
    @Mapping(target = "longitude", source = "location.x")
    public abstract MonumentDto toResponseDto(Monument monument);

    public abstract MonumentListDto toListDto(Monument monument);

    // 2. DTO -> Entity (Create)
    @Mapping(target = "location", expression = "java(toPoint(dto))")
    @Mapping(target = "division", ignore = true) // Handled by @AfterMapping or Service
    public abstract Monument toEntity(MonumentRequestDto dto);

    // 3. Update Existing Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true) // Usually immutable
    @Mapping(target = "location", expression = "java(toPoint(dto))")
    @Mapping(target = "division", ignore = true)
    public abstract void updateEntityFromDto(MonumentRequestDto dto, @MappingTarget Monument entity);

    // Helper for Point conversion
    protected Point toPoint(MonumentRequestDto dto) {
        return GeometryUtils.createPoint(dto.latitude(), dto.longitude());
    }

    /**
     * PRO TIP: Instead of MapStruct trying to create a 'New' Division with just an ID,
     * use an @AfterMapping to ensure the entity relationship is handled safely,
     * or handle this in the Service layer using repository.getReferenceById().
     */
    @AfterMapping
    protected void linkDivision(MonumentRequestDto dto, @MappingTarget Monument entity) {
        if (dto.divisionId() != null) {
            // This assumes your Service will ensure the division exists
            // or you inject a Repository here (not recommended for Mappers)
        }
    }
}