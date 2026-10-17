package pt.estga.processing.services.similarity;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.mark.api.MarkEvidenceQueryService;
import pt.estga.mark.dtos.EvidenceMarkDto;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.processing.config.ProcessingProperties;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimilarityServiceTest {

    private static final Long MARK_A = Long.valueOf(10L);
    private static final Long MARK_B = Long.valueOf(11L);

    @Mock
    private MarkEvidenceQueryService evidenceQueryService;

    private SimilarityService service;

    @BeforeEach
    void setUp() {
        ProcessingProperties properties = new ProcessingProperties(new ProcessingProperties.Similarity(20, 0.5));
        service = new SimilarityService(new SimpleMeterRegistry(), evidenceQueryService, properties);
    }

    private static MarkEvidenceProcessing seededProcessing() {
        return MarkEvidenceProcessing.builder()
                .id(UUID.randomUUID())
                .embedding(new float[]{1f, 0f, 0f})
                .build();
    }

    @Test
    @DisplayName("groups evidence hits by mark, sums confidence and sorts descending")
    void groupsAndScoresByMark() {
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();

        when(evidenceQueryService.findTopKSimilar(anyString(), anyInt(), anyDouble()))
                .thenReturn(List.of(
                        new MarkEvidenceDistanceDto(e1, 1L, Double.valueOf(0.9)),
                        new MarkEvidenceDistanceDto(e2, 2L, Double.valueOf(0.8))));
        when(evidenceQueryService.findMarksByEvidenceIds(anyList()))
                .thenReturn(List.of(
                        new EvidenceMarkDto(e1, MARK_A),
                        new EvidenceMarkDto(e1, MARK_B),
                        new EvidenceMarkDto(e2, MARK_A)));

        List<MarkSuggestion> result = service.findSimilar(seededProcessing(), 20);

        assertThat(result).extracting(MarkSuggestion::getMarkId).containsExactly(MARK_A, MARK_B);
        assertThat(result.get(0).getConfidence()).isEqualTo(1.0);   // 0.9 + 0.8 clamped to 1.0
        assertThat(result.get(1).getConfidence()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("returns no suggestions when the processing record has no embedding")
    void emptyWhenNoEmbedding() {
        MarkEvidenceProcessing processing = MarkEvidenceProcessing.builder().id(UUID.randomUUID()).build();

        assertThat(service.findSimilar(processing, 20)).isEmpty();
        verifyNoInteractions(evidenceQueryService);
    }

    @Test
    @DisplayName("discards non-finite similarity scores")
    void discardsNonFiniteScores() {
        UUID e1 = UUID.randomUUID();
        when(evidenceQueryService.findTopKSimilar(anyString(), anyInt(), anyDouble()))
                .thenReturn(List.of(new MarkEvidenceDistanceDto(e1, 1L, Double.valueOf(Double.NaN))));

        assertThat(service.findSimilar(seededProcessing(), 20)).isEmpty();
    }
}
