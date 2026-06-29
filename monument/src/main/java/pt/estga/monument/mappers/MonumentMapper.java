package pt.estga.monument.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.commoncore.enums.EntityStatus;
import pt.estga.commoncore.interfaces.DivisionLookupClient;
import pt.estga.commoncore.models.DivisionRef;
import pt.estga.monument.dtos.MonumentDto;
import pt.estga.monument.dtos.MonumentListDto;
import pt.estga.monument.dtos.MonumentRequestDto;
import pt.estga.monument.entities.Monument;
import pt.estga.monument.utils.GeometryUtils;

@Component
@RequiredArgsConstructor
public class MonumentMapper {

    private final DivisionLookupClient divisionLookupClient;

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
                toDivisionRef(monument.getDivisionCode()),
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
                toDivisionRef(monument.getDivisionCode()),
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
        monument.setDivisionCode(dto.divisionCode());
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
        entity.setDivisionCode(dto.divisionCode());
        if (dto.active() != null) {
            if (dto.active()) {
                entity.activate();
            } else {
                entity.deactivate();
            }
        }
    }

    private DivisionRef toDivisionRef(String code) {
        if (code == null || code.isBlank()) return null;
        String name = divisionLookupClient.findByCode(code)
                .map(DivisionRef::name)
                .orElse(null);
        return new DivisionRef(code, name);
    }
}
