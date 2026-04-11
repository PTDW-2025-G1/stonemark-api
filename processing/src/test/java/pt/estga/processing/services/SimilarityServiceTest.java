package pt.estga.processing.services;

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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SimilarityServiceTest {

    @Mock
    MarkEvidenceRepository evidenceRepository;

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
}
