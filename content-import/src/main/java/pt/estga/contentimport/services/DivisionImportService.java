package pt.estga.contentimport.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.entities.Country;
import pt.estga.territory.repositories.AdministrativeDivisionRepository;
import pt.estga.territory.services.CountryService;
import pt.estga.territory.exceptions.UnsupportedCountryException;
import pt.estga.territory.services.DivisionParentMatchingService;
import pt.estga.contentimport.mappers.DivisionFeatureMapper;

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

    private final AdministrativeDivisionRepository administrativeDivisionRepository;
    private final ObjectMapper objectMapper;
    private final DivisionParentMatchingService divisionParentMatchingService;
    private final CountryService countryService;
    private final DivisionFeatureMapper featureMapper;

    public int importFromPbf(InputStream pbfStream, String countryCode) throws Exception {

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

            // Resolve provided country code to id using CountryService (normalization handled there)
            Country providedCountry = countryService.findByCode(countryCode);
            if (providedCountry == null) {
                throw new UnsupportedCountryException("Provided countryCode is missing or unknown: " + countryCode);
            }
            Integer providedCountryId = providedCountry.getId();

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

                // Map feature to domain object using dedicated mapper
                Optional<AdministrativeDivision> mapped = featureMapper.mapFeature(feature, providedCountryId);
                if (mapped.isEmpty()) continue;

                AdministrativeDivision div = mapped.get();
                Optional<AdministrativeDivision> existingOpt = administrativeDivisionRepository.findById(div.getId());

                if (existingOpt.isPresent()) {
                    AdministrativeDivision existing = existingOpt.get();
                    existing.setName(div.getName());
                    existing.setOsmAdminLevel(div.getOsmAdminLevel());
                    existing.setGeometry(div.getGeometry());
                    existing.setCountry(div.getCountry());
                    batch.add(existing);
                } else {
                    batch.add(div);
                }
                count++;

                if (batch.size() == 1000) {
                    administrativeDivisionRepository.saveAll(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                administrativeDivisionRepository.saveAll(batch);
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
}
