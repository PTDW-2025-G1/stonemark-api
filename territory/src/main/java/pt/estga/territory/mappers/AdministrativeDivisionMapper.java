package pt.estga.territory.mappers;

import org.springframework.stereotype.Component;
import pt.estga.territory.dtos.AdministrativeDivisionDto;
import pt.estga.territory.entities.AdministrativeDivision;

import java.util.List;

@Component
public class AdministrativeDivisionMapper {

    public AdministrativeDivisionDto toDto(AdministrativeDivision entity) {
        if (entity == null) return null;
        return new AdministrativeDivisionDto(
                entity.getId(),
                entity.getName(),
                entity.getParent() != null ? entity.getParent().getId() : null,
                null
        );
    }

    public List<AdministrativeDivisionDto> toDtoList(List<AdministrativeDivision> entities) {
        if (entities == null) return List.of();
        return entities.stream().map(this::toDto).toList();
    }
}
