package pt.estga.territory.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.sharedweb.models.PagedRequest;
import pt.estga.territory.dtos.AdministrativeDivisionDto;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.mappers.AdministrativeDivisionMapper;
import pt.estga.territory.services.AdministrativeDivisionQueryService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/public/divisions")
@RequiredArgsConstructor
@Tag(name = "Administrative Divisions", description = "Endpoints for administrative divisions.")
public class AdministrativeDivisionController {

    private final AdministrativeDivisionQueryService service;
    private final AdministrativeDivisionMapper mapper;

    @PostMapping("/search")
    public ResponseEntity<Page<AdministrativeDivisionDto>> search(@RequestBody PagedRequest request) {
        return ResponseEntity.ok(service.search(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdministrativeDivisionDto> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<AdministrativeDivisionDto>> getDivisionsByType(
            @RequestParam String type,
            @RequestParam(required = false) boolean withMonuments
    ) {
        List<AdministrativeDivision> divisions;
        int adminLevel = switch (type.toLowerCase()) {
            case "district" -> 6;
            case "municipality" -> 7;
            case "parish" -> 8;
            default -> throw new IllegalArgumentException("Invalid type: " + type);
        };
        if (withMonuments) {
            divisions = service.findWithMonuments(adminLevel);
        } else {
            divisions = service.findByOsmAdminLevel(adminLevel);
        }
        return ResponseEntity.ok(mapper.toDtoList(divisions));
    }

    @GetMapping("/districts/{districtId}/municipalities")
    public ResponseEntity<List<AdministrativeDivisionDto>> getMunicipalitiesByDistrict(@PathVariable Long districtId) {
        List<AdministrativeDivision> municipalities = service.findChildren(districtId);
        log.info("Municipalities: {}", municipalities);
        return ResponseEntity.ok(mapper.toDtoList(municipalities));
    }

    @GetMapping("/municipalities/{municipalityId}/parishes")
    public ResponseEntity<List<AdministrativeDivisionDto>> getParishesByMunicipality(@PathVariable Long municipalityId) {
        List<AdministrativeDivision> parishes = service.findChildren(municipalityId);
        log.info("Parishes: {}", parishes);
        return ResponseEntity.ok(mapper.toDtoList(parishes));
    }

    @GetMapping("/municipalities/{municipalityId}/district")
    public ResponseEntity<AdministrativeDivisionDto> getDistrictByMunicipality(@PathVariable Long municipalityId) {
        return service.findParent(municipalityId)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/parishes/{parishId}/municipality")
    public ResponseEntity<AdministrativeDivisionDto> getMunicipalityByParish(@PathVariable Long parishId) {
        return service.findParent(parishId)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/coordinates")
    public ResponseEntity<List<AdministrativeDivisionDto>> getDivisionsByCoordinates(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        List<AdministrativeDivision> divisions = service.findByCoordinates(latitude, longitude);
        return ResponseEntity.ok(mapper.toDtoList(divisions));
    }
}
