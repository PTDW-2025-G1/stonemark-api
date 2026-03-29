package pt.estga.contentimport.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;
import pt.estga.monument.Monument;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonumentFeatureMapper {

    /**
     * Map a GeoJSON feature node to a Monument entity instance.
     * Returns Optional.empty() when the feature is invalid or should be skipped.
     */
    public Optional<Monument> mapFeature(JsonNode feature) {
        if (feature == null) return Optional.empty();

        JsonNode properties = feature.path("properties");
        if (!properties.isObject()) return Optional.empty();

        String externalId = feature.path("id").asText(null);
        if (externalId == null || externalId.isBlank()) {
            log.error("Found a feature with a missing or blank 'id'. Feature: {}", feature);
            return Optional.empty();
        }

        String name = properties.path("name").asText(null);
        if (name == null || name.isBlank()) {
            log.warn("Skipping feature with externalId '{}' because it has no name.", externalId);
            return Optional.empty();
        }

        JsonNode geometry = feature.path("geometry");
        if (!geometry.isObject()) return Optional.empty();

        JsonNode coordinates = geometry.path("coordinates");
        if (!coordinates.isArray() || coordinates.size() != 2) return Optional.empty();

        double lon = coordinates.get(0).asDouble(Double.NaN);
        double lat = coordinates.get(1).asDouble(Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lon)) return Optional.empty();

        // Create Point location with SRID 4326
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        Point point = gf.createPoint(new Coordinate(lon, lat));

        Monument monument = new Monument();
        monument.setExternalId(externalId);
        monument.setName(name);
        monument.setDescription(properties.path("description").asText(null));
        monument.setLocation(point);
        monument.setWebsite(properties.path("website").asText(null));
        monument.setProtectionTitle(properties.path("protection_title").asText(null));
        monument.setStreet(properties.path("addr:street").asText(null));
        monument.setHouseNumber(properties.path("addr:housenumber").asText(null));

        return Optional.of(monument);
    }
}
