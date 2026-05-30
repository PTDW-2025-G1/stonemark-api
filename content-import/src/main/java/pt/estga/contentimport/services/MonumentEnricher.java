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
     * Enrich the monument by setting its administrative division based on coordinates.
     * The repository returns divisions ordered by area ascending (smallest = most specific),
     * so the first result is the deepest/narrowest division containing the point.
     */
    public void enrichWithDivisions(Monument m) {
        if (m.getLocation() == null) return;
        double lon = m.getLocation().getX();
        double lat = m.getLocation().getY();
        List<AdministrativeDivision> divisions = administrativeDivisionRepository.findByCoordinates(lat, lon);
        if (divisions == null || divisions.isEmpty()) {
            m.setDivision(null);
            return;
        }

        m.setDivision(divisions.getFirst());
    }
}
