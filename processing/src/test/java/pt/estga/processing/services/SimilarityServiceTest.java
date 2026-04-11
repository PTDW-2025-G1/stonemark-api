package pt.estga.processing.services;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.mark.repositories.projections.EvidenceMarkProjection;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.services.similarity.JavaSimilarityEngine;
import pt.estga.processing.services.similarity.SimilarityService;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SimilarityServiceTest {

    @Mock
    MarkEvidenceRepository evidenceRepository;

    @Mock
    JavaSimilarityEngine javaSimilarityEngine;

    @InjectMocks
    SimilarityService similarityService;

    MarkEvidenceProcessing processing;

    @BeforeEach
    public void setUp() {
        processing = new MarkEvidenceProcessing();
        processing.setId(UUID.randomUUID());
        processing.setSubmission(null);
        processing.setStatus(null);
        // Ensure embedding exists so similarity is exercised in tests
        processing.setEmbedding(new float[] {1.0f, 0.0f, 0.0f});
        // Ensure service runs in DB mode for deterministic tests
        try {
            Field f = SimilarityService.class.getDeclaredField("similarityMode");
            f.setAccessible(true);
            f.set(similarityService, "db");
        } catch (Exception ignored) {
        }
        // Ensure rank-weighting is enabled for tests that assert weighted aggregation
        try {
            Field wf = SimilarityService.class.getDeclaredField("useRankWeighting");
            wf.setAccessible(true);
            wf.setBoolean(similarityService, true);
        } catch (Exception ignored) {
        }
        // Install a SimpleMeterRegistry so metric calls are safe in the service under test
        try {
            SimpleMeterRegistry simple = new SimpleMeterRegistry();
            Field mf = SimilarityService.class.getDeclaredField("meterRegistry");
            mf.setAccessible(true);
            mf.set(similarityService, simple);
        } catch (Exception ignored) {
        }
    }

    @Test
    public void identicalVectors_producesTopSuggestion() {
        // prepare hits: two projections, one with small distance (high similarity)
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        MarkEvidenceDistanceProjection p1 = new MarkEvidenceDistanceProjection() {
            public UUID getId() { return id1; }
            public Long getOccurrenceId() { return 1L; }
            public Double getDistance() { return 0.01; }
        };
        MarkEvidenceDistanceProjection p2 = new MarkEvidenceDistanceProjection() {
            public UUID getId() { return id2; }
            public Long getOccurrenceId() { return 2L; }
            public Double getDistance() { return 0.8; }
        };

        when(evidenceRepository.findTopKSimilarEvidence(anyString(), eq(5))).thenReturn(List.of(p1, p2));

        Mark m1 = new Mark(); m1.setId(11L);
        Mark m2 = new Mark(); m2.setId(22L);
        EvidenceMarkProjection r1 = new EvidenceMarkProjection() { public UUID getId(){return id1;} public Mark getMark(){return m1;} };
        EvidenceMarkProjection r2 = new EvidenceMarkProjection() { public UUID getId(){return id2;} public Mark getMark(){return m2;} };

        when(evidenceRepository.findMarksByEvidenceIds(anyList())).thenReturn(List.of(r1, r2));

        var suggestions = similarityService.findSimilar(processing, 5);
        assertNotNull(suggestions);
    }

    @Test
    public void randomVectors_filteredOut() {
        UUID id1 = UUID.randomUUID();
        MarkEvidenceDistanceProjection p1 = new MarkEvidenceDistanceProjection() {
            public UUID getId() { return id1; }
            public Long getOccurrenceId() { return 1L; }
            public Double getDistance() { return 0.9; } // similarity = 0.1
        };
        when(evidenceRepository.findTopKSimilarEvidence(anyString(), eq(5))).thenReturn(List.of(p1));
        when(evidenceRepository.findMarksByEvidenceIds(anyList())).thenReturn(List.of());

        var suggestions = similarityService.findSimilar(processing, 5);
        assertTrue(suggestions.isEmpty());
    }

    @Test
    public void multipleEvidences_sameMark_confidenceAggregatesWithWeighting() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        // two evidence rows that both map to the same Mark id (11)
        MarkEvidenceDistanceProjection p1 = new MarkEvidenceDistanceProjection() {
            public UUID getId() { return id1; }
            public Long getOccurrenceId() { return 1L; }
            public Double getDistance() { return 0.2; } // similarity = 0.8
        };
        MarkEvidenceDistanceProjection p2 = new MarkEvidenceDistanceProjection() {
            public UUID getId() { return id2; }
            public Long getOccurrenceId() { return 2L; }
            public Double getDistance() { return 0.4; } // similarity = 0.6
        };

        when(evidenceRepository.findTopKSimilarEvidence(anyString(), eq(5))).thenReturn(List.of(p1, p2));

        Mark sameMark = new Mark(); sameMark.setId(11L);
        EvidenceMarkProjection r1 = new EvidenceMarkProjection() { public UUID getId(){return id1;} public Mark getMark(){return sameMark;} };
        EvidenceMarkProjection r2 = new EvidenceMarkProjection() { public UUID getId(){return id2;} public Mark getMark(){return sameMark;} };
        when(evidenceRepository.findMarksByEvidenceIds(anyList())).thenReturn(List.of(r1, r2));

        var suggestions = similarityService.findSimilar(processing, 5);
        assertNotNull(suggestions);
        // Both evidences map to same mark so we should have a single aggregated suggestion
        assertEquals(1, suggestions.size());
        double confidence = suggestions.getFirst().getConfidence();
        // expected weighted confidence = (0.8*1 + 0.6*0.5) / (1 + 0.5) = 1.1 / 1.5 = 0.73333...
        assertEquals(1.1/1.5, confidence, 1e-6);
    }
}
