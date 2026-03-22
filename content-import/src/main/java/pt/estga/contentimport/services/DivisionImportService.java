package pt.estga.contentimport.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.springframework.stereotype.Service;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.repositories.AdministrativeDivisionRepository;
import pt.estga.territory.services.DivisionParentMatchingService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DivisionImportService {

    private final AdministrativeDivisionRepository repository;
    private final ObjectMapper objectMapper;
    private final DivisionParentMatchingService divisionParentMatchingService;

    public int importFromPbf(InputStream pbfStream) throws Exception {

        Path pbfFile = Files.createTempFile("portugal-admin-", ".osm.pbf");
        try {
            Files.copy(pbfStream, pbfFile, StandardCopyOption.REPLACE_EXISTING);

            ProcessBuilder pb = new ProcessBuilder(
                    "osmium", "export",
                    pbfFile.toAbsolutePath().toString(),
                    "--geometry-types=polygon",
                    "--attributes=type,id,version,timestamp,changeset,uid,user",
                    "--output-format=geojsonseq"
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            List<AdministrativeDivision> batch = new ArrayList<>(1000);
            int count = 0;
            GeoJsonReader geoJsonReader = new GeoJsonReader();

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (line.charAt(0) == 0x1E) {
                    line = line.substring(1);
                }

                JsonNode feature;
                try {
                    feature = objectMapper.readTree(line);
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse GeoJSON line from osmium: {}", line);
                    throw new IOException("Failed to parse GeoJSON output from osmium. The line was: '" + line + "'. Make sure 'osmium-tool' is installed and in the system's PATH.", e);
                }

                JsonNode props = feature.get("properties");
                JsonNode geomNode = feature.get("geometry");

                if (props == null || geomNode == null) continue;

                if (!"administrative".equals(props.path("boundary").asText())) {
                    continue;
                }

                if (!props.hasNonNull("admin_level")) {
                    continue;
                }
                int adminLevel = props.path("admin_level").asInt(-1);

                String name = resolveName(props);
                
                if (name == null || name.isBlank()) {
                    continue;
                }
                
                if (adminLevel != 6 && adminLevel != 7 && adminLevel != 8) {
                    continue;
                }

                long osmId = props.path("@id").asLong(0);
                String typeStr = props.path("@type").asText();

                if (osmId == 0 || typeStr.isBlank()) {
                    log.warn("Skipping division '{}' because of missing OSM ID or type from properties. ID: {}, Type: {}", name, osmId, typeStr);
                    continue;
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

                Optional<AdministrativeDivision> existingOpt = repository.findById(osmId);
                AdministrativeDivision div;

                if (existingOpt.isPresent()) {
                    div = existingOpt.get();
                    div.setName(name);
                    div.setOsmAdminLevel(adminLevel);
                    div.setGeometry(geometry);
                    div.setCountryCode("PT");
                } else {
                    div = AdministrativeDivision.builder()
                            .id(osmId)
                            .osmAdminLevel(adminLevel)
                            .name(name)
                            .geometry(geometry)
                            .countryCode("PT")
                            .build();
                }

                batch.add(div);
                count++;

                if (batch.size() == 1000) {
                    repository.saveAll(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                repository.saveAll(batch);
            }

            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException("osmium failed with exit code " + exit);
            }
            
            divisionParentMatchingService.matchAllDivisions();
            
            return count;
        } finally {
            Files.deleteIfExists(pbfFile);
        }
    }

    private String resolveName(JsonNode props) {
        if (props.hasNonNull("name")) {
            return props.get("name").asText();
        }
        return null;
    }
}
