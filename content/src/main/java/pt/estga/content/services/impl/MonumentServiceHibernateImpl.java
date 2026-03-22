package pt.estga.content.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Monument;
import pt.estga.content.repositories.MonumentRepository;
import pt.estga.content.services.MonumentService;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.services.AdministrativeDivisionQueryService;
import pt.estga.territory.services.AdministrativeDivisionService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonumentServiceHibernateImpl implements MonumentService {

    private final MonumentRepository repository;
    private final AdministrativeDivisionQueryService queryService;
    private final AdministrativeDivisionService service;

    @Override
    public Optional<Monument> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public Monument create(Monument monument) {
        enrichWithDivisions(monument);
        Monument savedMonument = repository.save(monument);
        updateCounters(null, savedMonument);
        return savedMonument;
    }

    @Override
    @Transactional
    public Monument update(Monument monument) {
        Optional<Monument> existingMonument = repository.findById(monument.getId());
        enrichWithDivisions(monument);
        Monument updatedMonument = repository.save(monument);
        existingMonument.ifPresent(value -> updateCounters(value, updatedMonument));
        return updatedMonument;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.findById(id).ifPresent(monument -> {
            updateCounters(monument, null);
            repository.deleteById(id);
        });
    }

    @Override
    public void enrichWithDivisions(Monument m) {
        if (m.getLatitude() != null && m.getLongitude() != null) {
            List<AdministrativeDivision> divisions = queryService.findByCoordinates(m.getLatitude(), m.getLongitude());
            m.setParish(null);
            m.setMunicipality(null);
            m.setDistrict(null);
            for (AdministrativeDivision division : divisions) {
                switch (division.getOsmAdminLevel()) {
                    case 6 -> m.setDistrict(division);
                    case 7 -> m.setMunicipality(division);
                    case 8 -> m.setParish(division);
                }
            }
        }
    }

    private void updateCounters(Monument oldMonument, Monument newMonument) {
        AdministrativeDivision oldParish = oldMonument != null ? oldMonument.getParish() : null;
        AdministrativeDivision newParish = newMonument != null ? newMonument.getParish() : null;
        updateDivisionCounter(oldParish, newParish);

        AdministrativeDivision oldMunicipality = oldMonument != null ? oldMonument.getMunicipality() : null;
        AdministrativeDivision newMunicipality = newMonument != null ? newMonument.getMunicipality() : null;
        updateDivisionCounter(oldMunicipality, newMunicipality);

        AdministrativeDivision oldDistrict = oldMonument != null ? oldMonument.getDistrict() : null;
        AdministrativeDivision newDistrict = newMonument != null ? newMonument.getDistrict() : null;
        updateDivisionCounter(oldDistrict, newDistrict);
    }

    private void updateDivisionCounter(AdministrativeDivision oldDivision, AdministrativeDivision newDivision) {
        if (Objects.equals(oldDivision, newDivision)) {
            return;
        }
        if (oldDivision != null) {
            service.decrementMonumentsCount(oldDivision.getId());
        }
        if (newDivision != null) {
            service.incrementMonumentsCount(newDivision.getId());
        }
    }
}
