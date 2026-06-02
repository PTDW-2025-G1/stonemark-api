package pt.estga.review.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionSource;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.markapi.MarkService;
import pt.estga.monument.Monument;
import pt.estga.monument.MonumentRepository;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.ReviewGroup;
import pt.estga.processing.entities.SpatialCluster;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.enums.ReviewGroupStatus;
import pt.estga.processing.enums.SpatialClusterStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.processing.repositories.ReviewGroupRepository;
import pt.estga.processing.repositories.SpatialClusterRepository;
import pt.estga.processing.services.cluster.SpatialClusterService;
import pt.estga.processing.services.similarity.DeduplicationService;
import pt.estga.review.dtos.AcceptGroupRequest;
import pt.estga.review.dtos.DiscoveryContext;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.models.ResolutionResult;
import pt.estga.review.processors.GroupReviewProcessor;
import pt.estga.review.processors.ReviewProcessor;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
import pt.estga.shared.utils.VectorUtils;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndToEndReviewGroupTest {

    @Mock private MarkEvidenceSubmissionRepository submissionRepository;
    @Mock private MarkEvidenceProcessingRepository processingRepository;
    @Mock private MarkSuggestionRepository suggestionRepository;
    @Mock private MarkEvidenceReviewRepository reviewRepository;
    @Mock private ReviewGroupRepository reviewGroupRepository;
    @Mock private SpatialClusterRepository spatialClusterRepository;
    @Mock private MarkOccurrenceRepository occurrenceRepository;
    @Mock private MarkEvidenceRepository evidenceRepository;
    @Mock private MarkRepository markRepository;
    @Mock private MonumentRepository monumentRepository;
    @Mock private MarkService markService;
    @Mock private ReviewExecutor executor;
    @Mock private GroupReviewProcessor groupProcessor;

    private DeduplicationService deduplicationService;
    private SpatialClusterService spatialClusterService;
    private ReviewService reviewService;

    private static final float[] IDENTICAL_EMBEDDING = normalize(
            new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f});
    private static final double TEST_LAT = 37.1234;
    private static final double TEST_LON = -8.5678;

    @BeforeEach
    void setUp() throws Exception {
        deduplicationService = new DeduplicationService(
                markService, submissionRepository, processingRepository, reviewGroupRepository);
        setPrivateField(deduplicationService, "vectorMaxDistance", 0.05);
        setPrivateField(deduplicationService, "spatialRadiusMeters", 100.0);
        setPrivateField(deduplicationService, "topK", 10);

        spatialClusterService = new SpatialClusterService(
                spatialClusterRepository, submissionRepository,
                processingRepository, occurrenceRepository, evidenceRepository,
                markRepository, monumentRepository);
        setPrivateField(spatialClusterService, "upsertRadiusMeters", 50.0);

        reviewService = new ReviewService(
                spatialClusterService, submissionRepository, processingRepository,
                suggestionRepository, reviewRepository, reviewGroupRepository,
                List.of(groupProcessor), executor);
        setPrivateField(reviewService, "allowEmptyReview", true);
        setPrivateField(reviewService, "newMarkMaxSuggestionConfidence", 0.5);
    }

    @Test
    void fullHappyPath_dedupCreatesGroup_acceptPromotesCluster() {
        UUID fileId1 = UUID.randomUUID();
        UUID fileId2 = UUID.randomUUID();

        MarkEvidenceSubmission sub1 = buildSubmission(1L, fileId1, TEST_LAT, TEST_LON);
        MarkEvidenceSubmission sub2 = buildSubmission(2L, fileId2, TEST_LAT + 0.0001, TEST_LON + 0.0001);

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub1));
        when(submissionRepository.findById(2L)).thenReturn(Optional.of(sub2));

        UUID procId1 = UUID.randomUUID();
        MarkEvidenceProcessing proc1 = buildProcessing(procId1, 1L);

        when(processingRepository.findById(procId1)).thenReturn(Optional.of(proc1));
        when(processingRepository.findBySubmissionId(1L)).thenReturn(Optional.of(proc1));

        when(markService.findTopKSimilar(anyString(), anyInt(), anyDouble())).thenReturn(List.of());

        boolean absorbed1 = deduplicationService.tryAbsorbIntoGroup(proc1);

        assertFalse(absorbed1, "First submission should not find any match");
        assertEquals(ProcessingStatus.PROCESSING, proc1.getStatus(),
                "First submission should remain PROCESSING");

        UUID procId2 = UUID.randomUUID();
        MarkEvidenceProcessing proc2 = buildProcessing(procId2, 2L);

        when(processingRepository.findById(procId2)).thenReturn(Optional.of(proc2));
        when(processingRepository.findBySubmissionId(2L)).thenReturn(Optional.of(proc2));

        UUID evidenceId1 = UUID.randomUUID();
        when(markService.findTopKSimilar(
                eq(VectorUtils.toVectorLiteral(IDENTICAL_EMBEDDING)), eq(10), eq(0.05)))
                .thenReturn(List.of(
                        new pt.estga.mark.dtos.MarkEvidenceDistanceDto(evidenceId1, null, 0.98)));

        when(markService.findEvidenceByIdIn(List.of(evidenceId1)))
                .thenReturn(List.of(
                        new pt.estga.mark.dtos.MarkEvidenceDto(evidenceId1, fileId1, null, null, null)));

        when(submissionRepository.findByOriginalMediaFileIdIn(List.of(fileId1)))
                .thenReturn(List.of(sub1));

        when(processingRepository.findBySubmissionIdIn(List.of(1L)))
                .thenReturn(List.of(proc1));
        when(processingRepository.findBySubmissionIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(proc1, proc2));

        when(reviewGroupRepository.findOpenGroupNearby(
                eq(TEST_LAT + 0.0001), eq(TEST_LON + 0.0001), eq(200.0)))
                .thenReturn(Optional.empty());

        ArgumentCaptor<ReviewGroup> groupCaptor = ArgumentCaptor.forClass(ReviewGroup.class);
        when(reviewGroupRepository.save(groupCaptor.capture())).thenAnswer(invocation -> {
            ReviewGroup saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                try {
                    Field idField = ReviewGroup.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(saved, 100L);
                } catch (Exception ignored) {}
            }
            return saved;
        });

        boolean absorbed2 = deduplicationService.tryAbsorbIntoGroup(proc2);

        assertTrue(absorbed2, "Second submission should be absorbed into ReviewGroup");
        assertEquals(ProcessingStatus.REVIEW_PENDING, proc2.getStatus());
        assertNotNull(proc2.getReviewGroup());

        verify(reviewGroupRepository, atLeastOnce()).save(any(ReviewGroup.class));

        ReviewGroup group = groupCaptor.getValue();
        assertNotNull(group);

        when(reviewGroupRepository.findById(100L)).thenReturn(Optional.of(group));
        when(processingRepository.findByReviewGroupId(100L)).thenReturn(List.of(proc1, proc2));

        Mark defaultMark = Mark.builder().id(200L).title("Test Mark").build();
        lenient().when(groupProcessor.getSupportedType()).thenReturn(ReviewType.GROUP_DISCOVERY);
        when(groupProcessor.resolve(anyLong(), any(DiscoveryContext.class)))
                .thenReturn(new ResolutionResult(defaultMark, null));

        when(executor.execute(any(), eq(ReviewDecision.APPROVED), any(),
                any(ResolutionResult.class), any(UUID.class), any(ReviewGroup.class)))
                .thenAnswer(invocation -> {
                    MarkEvidenceReview r = new MarkEvidenceReview();
                    r.setId(invocation.getArgument(0, MarkEvidenceSubmission.class).getId() + 1000);
                    return r;
                });

        when(spatialClusterRepository.findActiveWithinDistance(
                eq(TEST_LAT), eq(TEST_LON), eq(50.0)))
                .thenReturn(Optional.empty());

        ArgumentCaptor<SpatialCluster> clusterCaptor = ArgumentCaptor.forClass(SpatialCluster.class);
        when(spatialClusterRepository.save(clusterCaptor.capture())).thenAnswer(invocation -> {
            SpatialCluster sc = invocation.getArgument(0);
            if (sc.getId() == null) {
                try {
                    Field idField = SpatialCluster.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(sc, 300L);
                } catch (Exception ignored) {}
            }
            return sc;
        });

        when(monumentRepository.save(any(Monument.class))).thenAnswer(invocation -> {
            Monument m = invocation.getArgument(0);
            if (m.getId() == null) {
                try {
                    Field idField = Monument.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(m, 400L);
                } catch (Exception ignored) {}
            }
            return m;
        });

        when(markRepository.save(any(Mark.class))).thenAnswer(invocation -> {
            Mark m = invocation.getArgument(0);
            if (m.getId() == null) {
                try {
                    Field idField = Mark.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(m, 500L);
                } catch (Exception ignored) {}
            }
            return m;
        });

        when(occurrenceRepository.save(any(MarkOccurrence.class))).thenAnswer(invocation -> {
            MarkOccurrence o = invocation.getArgument(0);
            if (o.getId() == null) {
                try {
                    Field idField = MarkOccurrence.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(o, 600L);
                } catch (Exception ignored) {}
            }
            return o;
        });

        AcceptGroupRequest acceptRequest = new AcceptGroupRequest(
                "Discovered Site", null, null, "Group accepted");
        reviewService.acceptGroup(100L, acceptRequest);

        assertEquals(ReviewGroupStatus.REVIEWED, group.getGroupStatus());
        assertEquals(ReviewDecision.APPROVED.getCode(), group.getDecision());

        verify(spatialClusterRepository).save(any(SpatialCluster.class));
        SpatialCluster cluster = clusterCaptor.getValue();
        assertNotNull(cluster);
        assertEquals(SpatialClusterStatus.ACTIVE, cluster.getClusterStatus());
        assertTrue(cluster.getLabel().startsWith("Unnamed Site near"),
                "Cluster label should be auto-generated");

        verify(monumentRepository, atLeastOnce()).save(any(Monument.class));
        verify(markRepository, atLeastOnce()).save(any(Mark.class));
        verify(occurrenceRepository, times(2)).save(any(MarkOccurrence.class));

        assertEquals(cluster, proc1.getSpatialCluster());
        assertEquals(cluster, proc2.getSpatialCluster());

        verify(executor, times(2)).execute(
                any(), eq(ReviewDecision.APPROVED), any(),
                any(ResolutionResult.class), any(UUID.class), any(ReviewGroup.class));
    }

    private static MarkEvidenceSubmission buildSubmission(Long id, UUID fileId, double lat, double lon) {
        MarkEvidenceSubmission s = new MarkEvidenceSubmission();
        s.setId(id);
        s.setOriginalMediaFileId(fileId);
        s.setLatitude(lat);
        s.setLongitude(lon);
        s.setStatus(SubmissionStatus.RECEIVED);
        s.setSubmissionSource(SubmissionSource.API);
        s.setSubmittedAt(Instant.now());
        return s;
    }

    private static MarkEvidenceProcessing buildProcessing(UUID id, Long submissionId) {
        MarkEvidenceProcessing p = new MarkEvidenceProcessing();
        p.setId(id);
        p.setSubmissionId(submissionId);
        p.setEmbedding(IDENTICAL_EMBEDDING);
        p.setStatus(ProcessingStatus.PROCESSING);
        return p;
    }

    private static float[] normalize(float[] raw) {
        float[] n = VectorUtils.normalize(raw);
        return n != null ? n : raw;
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
