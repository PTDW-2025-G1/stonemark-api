package pt.estga.content.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.content.repositories.MarkOccurrenceRepository;
import pt.estga.content.repositories.MarkRepository;
import pt.estga.content.repositories.projections.MarkSimilarityProjection;
import pt.estga.content.services.MarkSearchService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkSearchServiceImpl implements MarkSearchService {

    private final MarkRepository markRepository;
    private final MarkOccurrenceRepository markOccurrenceRepository;

    private static final double SIMILARITY_THRESHOLD = 0.8;

    @Override
    public List<String> searchMarks(float[] embeddedVector) {
        if (embeddedVector == null || embeddedVector.length == 0) {
            log.warn("Search called with null or empty embedding vector");
            return List.of();
        }

        log.debug("Searching for similar marks with embedding length: {}", embeddedVector.length);
        List<MarkSimilarityProjection> results = markRepository.findSimilarMarks(embeddedVector);

        log.info("Mark similarity search returned {} results from database", results.size());
        results.forEach(result ->
            log.debug("Found mark ID: {} with similarity: {}", result.getId(), result.getSimilarity())
        );

        List<String> filteredResults = results.stream()
                .filter(result -> {
                    boolean meetsThreshold = result.getSimilarity() >= SIMILARITY_THRESHOLD;
                    if (!meetsThreshold) {
                        log.debug("Mark ID: {} filtered out (similarity {} < threshold {})",
                                result.getId(), result.getSimilarity(), SIMILARITY_THRESHOLD);
                    }
                    return meetsThreshold;
                })
                .map(result -> String.valueOf(result.getId()))
                .collect(Collectors.toList());

        log.info("After filtering by threshold ({}), {} marks remain", SIMILARITY_THRESHOLD, filteredResults.size());
        return filteredResults;
    }

    @Override
    public List<String> searchOccurrences(float[] embeddedVector) {
        if (embeddedVector == null || embeddedVector.length == 0) {
            return List.of();
        }

        List<MarkSimilarityProjection> results = markOccurrenceRepository.findSimilarOccurrences(embeddedVector);

        return results.stream()
                .filter(result -> result.getSimilarity() >= SIMILARITY_THRESHOLD)
                .map(result -> String.valueOf(result.getId()))
                .collect(Collectors.toList());
    }
}
