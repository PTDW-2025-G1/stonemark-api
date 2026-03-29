package pt.estga.contentimport.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.monument.Monument;
import pt.estga.monument.MonumentRepository;
import pt.estga.contentimport.mappers.MonumentFeatureMapper;

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
    private final ObjectMapper objectMapper;
    private final MonumentFeatureMapper featureMapper;
    private final MonumentEnricher enricher;

    @Transactional
    public int importFromGeoJson(InputStream inputStream) throws IOException {

        JsonNode root = objectMapper.readTree(inputStream);
        JsonNode features = root.path("features");

        if (!features.isArray()) {
            return 0;
        }

        // Process monuments from GeoJSON using dedicated mapper and enricher
        Map<String, Monument> monumentMap = new LinkedHashMap<>();

        for (JsonNode feature : features) {
            featureMapper.mapFeature(feature).ifPresent(monument -> {
                // Enrich with divisions based on coordinates
                enricher.enrichWithDivisions(monument);

                if (monumentMap.containsKey(monument.getExternalId())) {
                    log.warn("Duplicate monument external ID found: '{}'. The last entry will be used.", monument.getExternalId());
                }
                monumentMap.put(monument.getExternalId(), monument);
            });
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
}
