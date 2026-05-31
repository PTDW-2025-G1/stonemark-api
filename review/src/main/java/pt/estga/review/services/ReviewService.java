package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.mappers.MarkSuggestionMapper;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.processing.entities.ReviewGroup;
import pt.estga.processing.repositories.ReviewGroupRepository;
import pt.estga.processing.services.cluster.SpatialClusterService;
import pt.estga.review.dtos.AcceptGroupRequest;
import pt.estga.review.dtos.DiscoveryContext;
import pt.estga.review.dtos.GroupResponseDto;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.processing.enums.ReviewGroupStatus;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.models.ResolutionResult;
import pt.estga.review.processors.ReviewProcessor;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final SpatialClusterService spatialClusterService;
    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final MarkSuggestionRepository suggestionRepository;
    private final MarkEvidenceReviewRepository markEvidenceReviewRepository;
    private final ReviewGroupRepository reviewGroupRepository;
    private final List<ReviewProcessor> processors;
    private final ReviewExecutor executor;

    @Value("${review.allow-empty-review:false}")
    private boolean allowEmptyReview;

    @Value("${review.new-mark.max-suggestion-confidence:0.5}")
    private double newMarkMaxSuggestionConfidence;

    @Transactional
    public MarkEvidenceReview acceptAsNew(Long submissionId, String newMarTitle, String comment) {
        var overview = processingRepository.findOverviewBySubmissionId(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Processing not found"));

        Double maxConf = suggestionRepository.findMaxConfidenceByProcessingId(overview.getId());
        if (maxConf != null && maxConf >= newMarkMaxSuggestionConfidence) {
            throw new IllegalStateException("Confident suggestions exist. You must review existing marks.");
        }

        DiscoveryContext ctx = new DiscoveryContext(newMarTitle, null, null, null, null);
        return performReview(submissionId, ReviewType.DISCOVERY, ctx, comment);
    }

    @Transactional
    public MarkEvidenceReview acceptSuggestion(Long submissionId, Long markId, String comment) {
        DiscoveryContext ctx = new DiscoveryContext(null, markId, null, null, null);
        return performReview(submissionId, ReviewType.MATCH, ctx, comment);
    }

    @Transactional
    public MarkEvidenceReview rejectAll(Long submissionId, String comment) {
        return performReview(submissionId, ReviewType.REJECTION, null, comment);
    }

    @Transactional
    public MarkEvidenceReview performReview(Long submissionId, ReviewType type, DiscoveryContext ctx, String comment) {
        var submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

        var overview = processingRepository.findOverviewBySubmissionId(submissionId)
                .orElseThrow(() -> new IllegalStateException("Submission " + submissionId + " not processed"));

        validateState(submissionId, overview.getStatus(), suggestionRepository.countByProcessingId(overview.getId()), type);

        ReviewProcessor processor = processors.stream()
                .filter(p -> p.getSupportedType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No processor for review type: " + type));

        ResolutionResult resolution = processor.resolve(submissionId, ctx);

        ReviewDecision decision = (type == ReviewType.REJECTION) ? ReviewDecision.REJECTED : ReviewDecision.APPROVED;
        try {
            return executor.execute(submission, decision, comment, resolution, overview.getId());
        } catch (DataIntegrityViolationException dive) {
            throw new IllegalStateException("Submission " + submissionId + " already reviewed", dive);
        }
    }

    /**
     * Accept an entire ReviewGroup. All submissions in the group receive the same mark/monument resolution
     * and are approved in a single transactional operation.
     */
    @Transactional
    public void acceptGroup(Long groupId, AcceptGroupRequest request) {
        ReviewGroup group = reviewGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewGroup " + groupId + " not found"));

        if (group.getGroupStatus() != ReviewGroupStatus.OPEN) {
            throw new IllegalStateException("Group " + groupId + " is not OPEN (status=" + group.getGroupStatus() + ")");
        }

        List<MarkEvidenceProcessing> members = processingRepository.findByReviewGroupId(groupId);
        if (members.isEmpty()) {
            throw new IllegalStateException("Group " + groupId + " has no member processing records");
        }

        ReviewProcessor processor = processors.stream()
                .filter(p -> p.getSupportedType() == ReviewType.GROUP_DISCOVERY)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No GROUP_DISCOVERY processor configured"));

        DiscoveryContext ctx = buildGroupContext(request, group);
        String comment = request.comment();

        ResolutionResult resolution = processor.resolve(members.getFirst().getSubmissionId(), ctx);

        for (MarkEvidenceProcessing member : members) {
            Long submissionId = member.getSubmissionId();
            MarkEvidenceSubmission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

            executor.execute(submission, ReviewDecision.APPROVED, comment, resolution,
                    member.getId(),
                    group);
        }

        group.setGroupStatus(ReviewGroupStatus.REVIEWED);
        group.setDecision(ReviewDecision.APPROVED.getCode());
        reviewGroupRepository.save(group);

        boolean hasMonument = resolution != null && resolution.monument() != null;
        spatialClusterService.promoteIfEligible(group, true, hasMonument);
    }

    private static DiscoveryContext buildGroupContext(AcceptGroupRequest request, ReviewGroup group) {
        org.locationtech.jts.geom.Point location = null;
        if (request.monumentName() != null && group.getCentroid() != null) {
            location = group.getCentroid();
        }
        return new DiscoveryContext(
                request.markTitle(),
                null,
                request.monumentName(),
                request.monumentId(),
                location);
    }

    /**
     * Reject an entire ReviewGroup. All submissions in the group are rejected at once.
     */
    @Transactional
    public void rejectGroup(Long groupId, String comment) {
        ReviewGroup group = reviewGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewGroup " + groupId + " not found"));

        if (group.getGroupStatus() != ReviewGroupStatus.OPEN) {
            throw new IllegalStateException("Group " + groupId + " is not OPEN (status=" + group.getGroupStatus() + ")");
        }

        List<MarkEvidenceProcessing> members = processingRepository.findByReviewGroupId(groupId);
        if (members.isEmpty()) {
            throw new IllegalStateException("Group " + groupId + " has no member processing records");
        }

        for (MarkEvidenceProcessing member : members) {
            Long submissionId = member.getSubmissionId();
            MarkEvidenceSubmission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

            executor.execute(submission, ReviewDecision.REJECTED, comment, new ResolutionResult(null, null),
                    member.getId(),
                    group);
        }

        group.setGroupStatus(ReviewGroupStatus.REVIEWED);
        group.setDecision(ReviewDecision.REJECTED.getCode());
        reviewGroupRepository.save(group);

        spatialClusterService.promoteIfEligible(group, false, false);
    }

    @Transactional(readOnly = true)
    public List<MarkSuggestionDto> getSuggestions(Long submissionId) {
        return processingRepository.findBySubmissionId(submissionId)
                .map(p -> suggestionRepository.findByProcessingId(p.getId())
                        .stream()
                        .map(MarkSuggestionMapper::toDto)
                        .toList())
                .orElseThrow(() -> new ResourceNotFoundException("Suggestions not found"));
    }

    @Transactional(readOnly = true)
    public ReviewDecision getReviewStatus(Long submissionId) {
        return markEvidenceReviewRepository.findBySubmissionId(submissionId)
                .map(MarkEvidenceReview::getDecision)
                .orElse(null);
    }

    /**
     * Returns all submissions grouped into an open ReviewGroup, if any.
     */
    @Transactional(readOnly = true)
    public ReviewGroup getGroup(Long groupId) {
        return reviewGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewGroup " + groupId + " not found"));
    }

    /**
     * Returns a DTO representation of a ReviewGroup with its member submission IDs.
     */
    @Transactional(readOnly = true)
    public GroupResponseDto getGroupDto(Long groupId) {
        ReviewGroup group = getGroup(groupId);
        List<MarkEvidenceProcessing> members = processingRepository.findByReviewGroupId(groupId);

        List<Long> submissionIds = members.stream()
                .map(MarkEvidenceProcessing::getSubmissionId)
                .toList();

        Double centroidLat = null;
        Double centroidLon = null;
        if (group.getCentroid() != null) {
            centroidLat = group.getCentroid().getY();
            centroidLon = group.getCentroid().getX();
        }

        return new GroupResponseDto(
                group.getId(),
                group.getGroupStatus(),
                group.getMemberCount(),
                centroidLat,
                centroidLon,
                group.getRadiusMeters(),
                group.getDecision(),
                submissionIds);
    }

    private void validateState(Long submissionId, ProcessingStatus status, long count, ReviewType reviewType) {
        if (status != ProcessingStatus.COMPLETED && status != ProcessingStatus.REVIEW_PENDING) {
            throw new IllegalStateException("Processing not ready.");
        }
        if (reviewType != ReviewType.DISCOVERY && !allowEmptyReview && count == 0) {
            throw new IllegalStateException("No suggestions available to review.");
        }
        if (markEvidenceReviewRepository.existsBySubmissionId(submissionId)) {
            throw new IllegalStateException("Submission already reviewed.");
        }
    }
}
