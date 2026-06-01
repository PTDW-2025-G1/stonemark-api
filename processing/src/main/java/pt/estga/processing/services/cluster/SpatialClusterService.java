package pt.estga.processing.services.cluster;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.monument.Monument;
import pt.estga.monument.MonumentRepository;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.ReviewGroup;
import pt.estga.processing.entities.SpatialCluster;
import pt.estga.processing.enums.SpatialClusterStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.SpatialClusterRepository;
import pt.estga.shared.enums.ValidationState;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpatialClusterService {

    private final SpatialClusterRepository spatialClusterRepository;
    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final MarkOccurrenceRepository occurrenceRepository;
    private final MarkEvidenceRepository evidenceRepository;
    private final MarkRepository markRepository;
    private final MonumentRepository monumentRepository;

    @Value("${cluster.upsert-radius-meters:50}")
    private double upsertRadiusMeters;

    /**
     * Promote a completed ReviewGroup to a SpatialCluster when no real Monument was linked.
     * Called by the review module after a group is fully reviewed and approved.
     */
    @Transactional
    public void promoteIfEligible(ReviewGroup group, boolean approved, boolean hasLinkedMonument) {
        if (!approved) {
            log.debug("Group {} rejected, skipping SpatialCluster promotion", group.getId());
            return;
        }
        if (hasLinkedMonument) {
            log.debug("Group {} linked to a real Monument, skipping SpatialCluster promotion", group.getId());
            return;
        }
        if (group.getCentroid() == null) {
            log.warn("Group {} has no centroid, cannot promote to SpatialCluster", group.getId());
            return;
        }

        double lat = group.getCentroid().getY();
        double lon = group.getCentroid().getX();

        SpatialCluster cluster = spatialClusterRepository.findActiveWithinDistance(lat, lon, upsertRadiusMeters)
                .orElseGet(() -> spatialClusterRepository.save(SpatialCluster.builder()
                        .centroid(group.getCentroid())
                        .radiusMeters(upsertRadiusMeters)
                        .label(generateLabel(lat, lon))
                        .clusterStatus(SpatialClusterStatus.ACTIVE)
                        .build()));

        linkMembersToCluster(cluster, group);
        linkSubmissionsToPhantomMonument(cluster, group);

        log.info("Promoted group {} to SpatialCluster {} (label='{}')",
                group.getId(), cluster.getId(), cluster.getLabel());
    }

    private void linkMembersToCluster(SpatialCluster cluster, ReviewGroup group) {
        List<MarkEvidenceProcessing> members = processingRepository.findByReviewGroupId(group.getId());
        for (MarkEvidenceProcessing member : members) {
            member.setSpatialCluster(cluster);
            processingRepository.save(member);
        }
    }

    private void linkSubmissionsToPhantomMonument(SpatialCluster cluster, ReviewGroup group) {
        Monument phantom = monumentRepository.save(Monument.builder()
                .name(cluster.getLabel())
                .location(cluster.getCentroid())
                .validationState(ValidationState.PROVISIONAL)
                .build());

        Mark defaultMark = markRepository.save(Mark.builder()
                .title(cluster.getLabel())
                .validationState(ValidationState.PROVISIONAL)
                .build());

        MarkOccurrence occurrence = occurrenceRepository.save(MarkOccurrence.builder()
                .mark(defaultMark)
                .monument(phantom)
                .validationState(ValidationState.PROVISIONAL)
                .build());

        List<MarkEvidenceProcessing> members = processingRepository.findByReviewGroupId(group.getId());
        for (MarkEvidenceProcessing member : members) {
            submissionRepository.findById(member.getSubmissionId()).ifPresent(submission -> {
                if (submission.getOriginalMediaFileId() == null) return;

                evidenceRepository.findByFileId(submission.getOriginalMediaFileId()).ifPresentOrElse(
                        evidence -> {
                            evidence.setOccurrence(occurrence);
                            evidenceRepository.save(evidence);
                        },
                        () -> {
                            MarkEvidence newEvidence = MarkEvidence.builder()
                                    .fileId(submission.getOriginalMediaFileId())
                                    .embedding(member.getEmbedding())
                                    .occurrence(occurrence)
                                    .build();
                            evidenceRepository.save(newEvidence);
                            log.info("Created MarkEvidence {} for phantom monument cluster {} (submission {})",
                                    newEvidence.getId(), cluster.getId(), submission.getId());
                        });
            });
        }
    }

    private static String generateLabel(double lat, double lon) {
        String latDir = lat >= 0 ? "N" : "S";
        String lonDir = lon >= 0 ? "E" : "W";
        return String.format("Unnamed Site near %.4f°%s, %.4f°%s",
                Math.abs(lat), latDir, Math.abs(lon), lonDir);
    }
}
