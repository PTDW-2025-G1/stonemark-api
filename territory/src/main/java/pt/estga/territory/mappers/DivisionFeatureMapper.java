package pt.estga.territory.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.springframework.stereotype.Component;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.entities.Country;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DivisionFeatureMapper {

    private final ObjectMapper objectMapper;

    private final GeoJsonReader geoJsonReader = new GeoJsonReader();

    public Optional<AdministrativeDivision> mapFeature(JsonNode feature, Integer providedCountryId) throws Exception {
        if (feature == null) return Optional.empty();

        JsonNode props = feature.get("properties");
        JsonNode geomNode = feature.get("geometry");
        if (props == null || geomNode == null) return Optional.empty();

        if (!"administrative".equals(props.path("boundary").asText())) return Optional.empty();

        String name = resolveName(props);
        if (name == null || name.isBlank()) return Optional.empty();

        long osmId = props.path("@id").asLong(0);
        String typeStr = props.path("@type").asText();
        if (osmId == 0 || typeStr.isBlank()) {
            log.warn("Skipping division '{}' because of missing OSM ID or type from properties. ID: {}, Type: {}", name, osmId, typeStr);
            return Optional.empty();
        }

        Geometry geometry = geoJsonReader.read(objectMapper.writeValueAsString(geomNode));
        geometry.setSRID(4326);

        if (!geometry.isValid()) {
            log.warn("Fixing invalid geometry for OSM ID: {}", osmId);
            geometry = geometry.buffer(0);
            if (!geometry.isValid()) {
                log.error("Geometry still invalid after buffer(0) for OSM ID: {}", osmId);
            }
        }

        AdministrativeDivision div = AdministrativeDivision.builder()
                .id(osmId)
                .name(name)
                .geometry(geometry)
                .country(providedCountryId == null ? null : Country.builder().id(providedCountryId).build())
                .build();

        return Optional.of(div);
    }

    private String resolveName(JsonNode props) {
        if (props.hasNonNull("name")) {
            return props.get("name").asText();
        }
        return null;
    }
}
