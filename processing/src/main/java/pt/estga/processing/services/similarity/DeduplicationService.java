package pt.estga.processing.services.similarity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.markapi.MarkService;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.ReviewGroup;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.enums.ReviewGroupStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.ReviewGroupRepository;
import pt.estga.shared.utils.VectorUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeduplicationService {

    private final MarkService markService;
    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final ReviewGroupRepository reviewGroupRepository;

    @Value("${processing.deduplication.vector-max-distance:0.05}")
    private double vectorMaxDistance;

    @Value("${processing.deduplication.spatial-radius-meters:100}")
    private double spatialRadiusMeters;

    @Value("${processing.deduplication.top-k:10}")
    private int topK;

    /**
     * Attempts to find an existing ReviewGroup that the given processing record should join.
     * If found, the processing record is linked to the group and set to REVIEW_PENDING instead
     * of COMPLETED, so the caller should skip standalone review generation.
     */
    @Transactional
    public boolean tryAbsorbIntoGroup(MarkEvidenceProcessing processing) {
        if (processing.getEmbedding() == null) return false;

        MarkEvidenceSubmission submission = submissionRepository.findById(processing.getSubmissionId()).orElse(null);
        if (submission == null || submission.getLatitude() == null || submission.getLongitude() == null) return false;

        double lat = submission.getLatitude();
        double lon = submission.getLongitude();

        String vectorLiteral = VectorUtils.toVectorLiteral(processing.getEmbedding());

        List<MarkEvidenceDistanceDto> candidates = markService.findTopKSimilar(vectorLiteral, topK, vectorMaxDistance);

        if (candidates.isEmpty()) return false;

        Set<UUID> candidateEvidenceIds = candidates.stream()
                .map(MarkEvidenceDistanceDto::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<UUID> candidateFileIds = markService.findEvidenceByIdIn(new ArrayList<>(candidateEvidenceIds))
                .stream()
                .map(MarkEvidenceDto::fileId)
                .filter(Objects::nonNull)
                .toList();

        if (candidateFileIds.isEmpty()) return false;

        List<MarkEvidenceSubmission> fileMatches = submissionRepository
                .findByOriginalMediaFileIdIn(candidateFileIds);

        if (fileMatches.isEmpty()) return false;

        List<Long> matchingSubmissionIds = fileMatches.stream()
                .map(MarkEvidenceSubmission::getId)
                .toList();

        List<MarkEvidenceProcessing> nearbyProcessings = processingRepository
                .findBySubmissionIdIn(matchingSubmissionIds);

        List<Long> spatiallyCloseSubmissionIds = new ArrayList<>();
        for (MarkEvidenceProcessing candidate : nearbyProcessings) {
            if (candidate.getId().equals(processing.getId())) continue;
            MarkEvidenceSubmission cs = submissionRepository.findById(candidate.getSubmissionId()).orElse(null);
            if (cs == null || cs.getLatitude() == null || cs.getLongitude() == null) continue;
            if (haversine(lat, lon, cs.getLatitude(), cs.getLongitude()) <= spatialRadiusMeters) {
                spatiallyCloseSubmissionIds.add(candidate.getSubmissionId());
            }
        }

        if (spatiallyCloseSubmissionIds.isEmpty()) return false;

        spatiallyCloseSubmissionIds.add(processing.getSubmissionId());

        Optional<ReviewGroup> existingGroup = reviewGroupRepository.findOpenGroupNearby(lat, lon, spatialRadiusMeters * 2);

        ReviewGroup group = existingGroup.orElseGet(() -> reviewGroupRepository.save(
                ReviewGroup.builder()
                        .groupStatus(ReviewGroupStatus.OPEN)
                        .centroid(pt.estga.territory.utils.GeometryUtils.createPoint(lat, lon))
                        .radiusMeters(spatialRadiusMeters)
                        .meanEmbedding(processing.getEmbedding())
                        .memberCount(0)
                        .build()
        ));

        List<MarkEvidenceProcessing> members = processingRepository.findBySubmissionIdIn(spatiallyCloseSubmissionIds);
        for (MarkEvidenceProcessing member : members) {
            member.setReviewGroup(group);
            if (!member.getId().equals(processing.getId())) {
                member.setStatus(ProcessingStatus.REVIEW_PENDING);
            }
            processingRepository.save(member);
        }

        processing.setReviewGroup(group);
        processing.setStatus(ProcessingStatus.REVIEW_PENDING);
        processingRepository.save(processing);

        int memberCount = (int) processingRepository.findBySubmissionIdIn(spatiallyCloseSubmissionIds)
                .stream().filter(p -> p.getReviewGroup() != null && p.getReviewGroup().getId().equals(group.getId())).count();
        group.setMemberCount(memberCount);

        recomputeGroupCentroid(group, members);

        log.info("Absorbed processing {} (submission {}) into ReviewGroup {} ({} members)",
                processing.getId(), processing.getSubmissionId(), group.getId(), group.getMemberCount());

        return true;
    }

    private void recomputeGroupCentroid(ReviewGroup group, List<MarkEvidenceProcessing> members) {
        double sumLat = 0, sumLon = 0;
        int count = 0;
        for (MarkEvidenceProcessing m : members) {
            MarkEvidenceSubmission s = submissionRepository.findById(m.getSubmissionId()).orElse(null);
            if (s != null && s.getLatitude() != null && s.getLongitude() != null) {
                sumLat += s.getLatitude();
                sumLon += s.getLongitude();
                count++;
            }
        }
        if (count > 0) {
            group.setCentroid(pt.estga.territory.utils.GeometryUtils.createPoint(sumLat / count, sumLon / count));
        }
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
