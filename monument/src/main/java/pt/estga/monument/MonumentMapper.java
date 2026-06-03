package pt.estga.monument;

import pt.estga.monument.entities.Monument;
import pt.estga.monument.dtos.MonumentDto;
import pt.estga.monument.dtos.MonumentListDto;
import pt.estga.monument.dtos.MonumentRequestDto;
import pt.estga.shared.enums.EntityStatus;
import pt.estga.territory.dtos.AdministrativeDivisionDto;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.utils.GeometryUtils;

public class MonumentMapper {

    private MonumentMapper() {}

    public static MonumentDto toResponseDto(Monument monument) {
        if (monument == null) return null;
        AdministrativeDivisionDto divisionDto = null;
        if (monument.getDivision() != null) {
            divisionDto = AdministrativeDivisionDto.builder()
                    .id(monument.getDivision().getId())
                    .name(monument.getDivision().getName())
                    .build();
        }
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
                divisionDto,
                monument.getCreatedAt(),
                monument.getLastModifiedAt(),
                monument.getStatus() == EntityStatus.ACTIVE
        );
    }

    public static MonumentListDto toListDto(Monument monument) {
        if (monument == null) return null;
        AdministrativeDivisionDto divisionDto = null;
        if (monument.getDivision() != null) {
            divisionDto = AdministrativeDivisionDto.builder()
                    .id(monument.getDivision().getId())
                    .name(monument.getDivision().getName())
                    .build();
        }
        return new MonumentListDto(
                monument.getId(),
                monument.getName(),
                divisionDto,
                monument.getStatus() == EntityStatus.ACTIVE
        );
    }

    public static Monument toEntity(MonumentRequestDto dto) {
        if (dto == null) return null;
        Monument monument = new Monument();
        monument.setName(dto.name());
        monument.setDescription(dto.description());
        monument.setProtectionTitle(dto.protectionTitle());
        monument.setWebsite(dto.website());
        monument.setLocation(GeometryUtils.createPoint(dto.latitude(), dto.longitude()));
        monument.setAddress(dto.address());
        monument.setPostalCode(dto.postalCode());
        if (dto.divisionId() != null) {
            monument.setDivision(AdministrativeDivision.builder().id(dto.divisionId()).build());
        }
        if (dto.active() != null && !dto.active()) {
            monument.deactivate();
        }
        return monument;
    }

    public static void updateEntityFromDto(MonumentRequestDto dto, Monument entity) {
        if (dto == null || entity == null) return;
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setProtectionTitle(dto.protectionTitle());
        entity.setWebsite(dto.website());
        entity.setLocation(GeometryUtils.createPoint(dto.latitude(), dto.longitude()));
        entity.setAddress(dto.address());
        entity.setPostalCode(dto.postalCode());
        if (dto.divisionId() != null) {
            entity.setDivision(AdministrativeDivision.builder().id(dto.divisionId()).build());
        } else {
            entity.setDivision(null);
        }
        if (dto.active() != null) {
            if (dto.active()) {
                entity.activate();
            } else {
                entity.deactivate();
            }
        }
    }
}
