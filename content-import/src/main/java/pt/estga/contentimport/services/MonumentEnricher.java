package pt.estga.contentimport.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.monument.Monument;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.repositories.AdministrativeDivisionRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MonumentEnricher {

    private final AdministrativeDivisionRepository administrativeDivisionRepository;

    /**
     * Enrich the monument by setting its administrative division based on coordinates.
     * Selects the division with the highest non-null osmAdminLevel; if none have a level,
     * falls back to the first non-null division returned by the repository.
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

        Optional<AdministrativeDivision> bestByLevel = divisions.stream()
            .filter(Objects::nonNull)
            .filter(d -> d.getOsmAdminLevel() != null)
            .max(Comparator.comparing(AdministrativeDivision::getOsmAdminLevel));

        AdministrativeDivision best = bestByLevel.orElseGet(() ->
            divisions.stream().filter(Objects::nonNull).findFirst().orElse(null)
        );

        m.setDivision(best);
    }
}
