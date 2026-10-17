package pt.estga.monument.mappers;

import org.springframework.stereotype.Component;
import pt.estga.monument.dtos.MonumentDto;
import pt.estga.monument.dtos.MonumentListDto;
import pt.estga.monument.dtos.MonumentRequestDto;
import pt.estga.monument.entities.Monument;
import pt.estga.shared.core.enums.EntityStatus;
import pt.estga.shared.core.utils.GeometryUtils;

@Component
public class MonumentMapper {

    public MonumentDto toResponseDto(Monument monument) {
        if (monument == null) return null;
        return new MonumentDto(
                monument.getId(),
                monument.getName(),
                monument.getDescription(),
                monument.getProtectionTitle(),
                monument.getWebsite(),
                monument.getLocation() != null ? monument.getLocation().getY() : null,
                monument.getLocation() != null ? monument.getLocation().getX() : null,
                monument.getAddress(),
                monument.getPostalCode(),
                monument.getCreatedAt(),
                monument.getLastModifiedAt(),
                monument.getStatus() == EntityStatus.ACTIVE
        );
    }

    public MonumentListDto toListDto(Monument monument) {
        if (monument == null) return null;
        return new MonumentListDto(
                monument.getId(),
                monument.getName(),
                monument.getStatus() == EntityStatus.ACTIVE
        );
    }

    public Monument toEntity(MonumentRequestDto dto) {
        if (dto == null) return null;
        Monument monument = new Monument();
        monument.setName(dto.name());
        monument.setDescription(dto.description());
        monument.setProtectionTitle(dto.protectionTitle());
        monument.setWebsite(dto.website());
        monument.setLocation(GeometryUtils.createPoint(dto.latitude(), dto.longitude()));
        monument.setAddress(dto.address());
        monument.setPostalCode(dto.postalCode());
        if (dto.active() != null && !dto.active()) {
            monument.deactivate();
        }
        return monument;
    }

    public void updateEntityFromDto(MonumentRequestDto dto, Monument entity) {
        if (dto == null || entity == null) return;
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setProtectionTitle(dto.protectionTitle());
        entity.setWebsite(dto.website());
        entity.setLocation(GeometryUtils.createPoint(dto.latitude(), dto.longitude()));
        entity.setAddress(dto.address());
        entity.setPostalCode(dto.postalCode());
        if (dto.active() != null) {
            if (dto.active()) {
                entity.activate();
            } else {
                entity.deactivate();
            }
        }
    }
}
