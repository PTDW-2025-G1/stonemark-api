package pt.estga.territory.mappers;

import pt.estga.territory.dtos.AdministrativeDivisionDto;
import pt.estga.territory.entities.AdministrativeDivision;

import java.util.List;

public class AdministrativeDivisionMapper {

    private AdministrativeDivisionMapper() {}

    public static AdministrativeDivisionDto toDto(AdministrativeDivision entity) {
        if (entity == null) return null;
        return new AdministrativeDivisionDto(
                entity.getId(),
                entity.getName(),
                entity.getParent() != null ? entity.getParent().getId() : null,
                null
        );
    }

    public static List<AdministrativeDivisionDto> toDtoList(List<AdministrativeDivision> entities) {
        if (entities == null) return List.of();
        return entities.stream().map(AdministrativeDivisionMapper::toDto).toList();
    }
}
