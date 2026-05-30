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
public class AdministrativeDivisionParentMatchingService {

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
        Optional<AdministrativeDivision> parentOpt = repository.findParentByGeometry(division.getId());
        parentOpt.ifPresent(parent -> {
            division.setParent(parent);
            repository.save(division);
        });

        if (parentOpt.isEmpty()) {
            log.warn("Could not find parent for division '{}'", division.getName());
        }
    }
}
