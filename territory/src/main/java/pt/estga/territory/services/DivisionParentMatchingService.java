package pt.estga.territory.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.repositories.AdministrativeDivisionRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DivisionParentMatchingService {

    private final AdministrativeDivisionRepository repository;

    @Async
    public void matchAllDivisions() {
        log.info("Starting parent division matching process...");

        List<AdministrativeDivision> divisionsWithoutParent = repository.findAllByParentIsNull();
        log.info("Found {} divisions without a parent.", divisionsWithoutParent.size());

        for (AdministrativeDivision division : divisionsWithoutParent) {
            matchDivision(division);
        }

        log.info("Finished parent division matching process.");
    }

    private void matchDivision(AdministrativeDivision division) {
        Integer parentAdminLevel = getParentAdminLevel(division.getOsmAdminLevel());
        if (parentAdminLevel == null) {
            return;
        }

        Optional<AdministrativeDivision> parentOpt = repository.findParentByGeometry(division.getId(), parentAdminLevel);
        parentOpt.ifPresent(parent -> {
            division.setParent(parent);
            repository.save(division);
        });

        if (parentOpt.isEmpty()) {
            log.warn("Could not find parent for division '{}' (Level {}) with expected parent admin level {}", division.getName(), division.getOsmAdminLevel(), parentAdminLevel);
        }
    }

    private Integer getParentAdminLevel(int adminLevel) {
        return switch (adminLevel) {
            case 8 -> // Parish
                    7; // Parent is Municipality
            case 7 -> // Municipality
                    6; // Parent is District
            case 6 -> // District
                    null;
            default -> null;
        };
    }
}
