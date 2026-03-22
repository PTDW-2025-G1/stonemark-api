package pt.estga.contentimport.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Monument;
import pt.estga.content.repositories.MonumentRepository;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.repositories.AdministrativeDivisionRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonumentImportService {

    private final MonumentRepository monumentRepository;
    private final AdministrativeDivisionRepository administrativeDivisionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public int importFromGeoJson(InputStream inputStream) throws IOException {

        JsonNode root = objectMapper.readTree(inputStream);
        JsonNode features = root.path("features");

        if (!features.isArray()) {
            return 0;
        }

        // Process monuments from GeoJSON
        Map<String, Monument> monumentMap = new LinkedHashMap<>();

        for (JsonNode feature : features) {
            JsonNode properties = feature.path("properties");
            if (!properties.isObject()) continue;

            String externalId = feature.path("id").asText(null);
            if (externalId == null || externalId.isBlank()) {
                log.error("Found a feature with a missing or blank 'id'. Halting import. Feature: {}", feature.toString());
                throw new IllegalArgumentException("Found a feature with a missing or blank 'id'. All features must have a unique 'id'.");
            }

            String name = properties.path("name").asText(null);
            if (name == null || name.isBlank()) {
                log.warn("Skipping feature with externalId '{}' because it has no name.", externalId);
                continue;
            }

            JsonNode geometry = feature.path("geometry");
            if (!geometry.isObject()) continue;

            JsonNode coordinates = geometry.path("coordinates");
            if (!coordinates.isArray() || coordinates.size() != 2) continue;

            double lon = coordinates.get(0).asDouble(Double.NaN);
            double lat = coordinates.get(1).asDouble(Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lon)) continue;

            Monument monument = new Monument();
            monument.setExternalId(externalId);
            monument.setName(name);
            monument.setDescription(properties.path("description").asText(null));
            monument.setLatitude(lat);
            monument.setLongitude(lon);
            monument.setWebsite(properties.path("website").asText(null));
            monument.setProtectionTitle(properties.path("protection_title").asText(null));
            monument.setStreet(properties.path("addr:street").asText(null));
            monument.setHouseNumber(properties.path("addr:housenumber").asText(null));

            // Set all divisions (parish, municipality, district) using standard service logic
            enrichWithDivisions(monument);

            if (monumentMap.containsKey(externalId)) {
                log.warn("Duplicate monument external ID found: '{}'. The last entry will be used.", externalId);
            }
            monumentMap.put(externalId, monument);
        }

        // Persist monuments
        List<Monument> toSave = new ArrayList<>();
        for (Monument incoming : monumentMap.values()) {
            monumentRepository.findByExternalId(incoming.getExternalId())
                    .ifPresentOrElse(
                            existing -> {
                                incoming.setId(existing.getId()); // Set ID to trigger an update
                                toSave.add(incoming);
                            },
                            () -> toSave.add(incoming)
                    );
        }

        return monumentRepository.saveAll(toSave).size();
    }

    public void enrichWithDivisions(Monument m) {
        if (m.getLatitude() != null && m.getLongitude() != null) {
            List<AdministrativeDivision> divisions = administrativeDivisionRepository.findByCoordinates(m.getLatitude(), m.getLongitude());
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
}
