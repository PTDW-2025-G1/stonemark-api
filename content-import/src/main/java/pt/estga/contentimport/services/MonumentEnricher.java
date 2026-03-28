package pt.estga.contentimport.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.monument.Monument;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.repositories.AdministrativeDivisionRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MonumentEnricher {

    private final AdministrativeDivisionRepository administrativeDivisionRepository;

    /**
     * Enrich the monument with parish, municipality and district based on coordinates.
     * Clears existing associations before setting found divisions.
     */
    public void enrichWithDivisions(Monument m) {
        if (m.getLocation() == null) return;
        double lon = m.getLocation().getX();
        double lat = m.getLocation().getY();
        List<AdministrativeDivision> divisions = administrativeDivisionRepository.findByCoordinates(lat, lon);
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
