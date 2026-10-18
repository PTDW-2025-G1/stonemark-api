package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.api.IntakeSubmissionApi;
import pt.estga.intake.api.SubmissionRef;
import pt.estga.mark.api.MarkQueryService;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.processing.api.ProcessingReviewApi;
import pt.estga.processing.api.SubmissionEmbedding;
import pt.estga.review.dtos.CreateMarkFromSuggestionRequest;
import pt.estga.review.dtos.MarkCreationSuggestionDto;
import pt.estga.review.entities.MarkCreationSuggestion;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.MarkCreationSuggestionStatus;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.repositories.MarkCreationSuggestionRepository;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
import pt.estga.shared.web.exceptions.ResourceNotFoundException;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Turns clusters of unmatched submissions into mark-creation suggestions.
 *
 * <p>A submission that a moderator marks as {@code NO_MATCH} becomes a candidate.
 * Candidates whose Vision embeddings are close enough form a cluster; when a
 * cluster reaches {@code review.mark-creation.min-cluster-size}, it is proposed to
 * moderators. No mark is created until a moderator acts on the suggestion.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkCreationSuggestionService {

    private final MarkEvidenceReviewRepository reviewRepository;
    private final MarkCreationSuggestionRepository suggestionRepository;
    private final ProcessingReviewApi processingApi;
    private final IntakeSubmissionApi intakeApi;
    private final MarkQueryService markQueryService;

    @Value("${review.mark-creation.min-cluster-size:3}")
    private int minClusterSize;

    @Value("${review.mark-creation.max-distance:0.15}")
    private double maxDistance;

    public Page<MarkCreationSuggestionDto> list(MarkCreationSuggestionStatus status, Pageable pageable) {
        return suggestionRepository.findByStatus(status, pageable).map(MarkCreationSuggestionService::toDto);
    }

    /**
     * Recomputes open suggestions from the current candidates. Idempotent: existing
     * open suggestions are refreshed, stale ones are dropped, and clusters a
     * moderator dismissed are never proposed again.
     */
    @Transactional
    public void recompute() {
        List<UUID> candidateSubmissionIds = reviewRepository.findByDecision(ReviewDecision.NO_MATCH).stream()
                .map(MarkEvidenceReview::getSubmissionId)
                .toList();
        if (candidateSubmissionIds.isEmpty()) {
            suggestionRepository.deleteByStatus(MarkCreationSuggestionStatus.OPEN);
            return;
        }

        List<SubmissionEmbedding> embeddings = processingApi.findEmbeddings(candidateSubmissionIds);
        List<Cluster> clusters = cluster(embeddings).stream()
                .filter(cluster -> cluster.members().size() >= minClusterSize)
                .toList();
        reconcile(clusters);
    }

    @Transactional
    public MarkCreationSuggestionDto createMark(UUID suggestionId, CreateMarkFromSuggestionRequest request) {
        MarkCreationSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Suggestion " + suggestionId + " not found"));
        if (suggestion.getStatus() != MarkCreationSuggestionStatus.OPEN) {
            throw new IllegalStateException("Suggestion is not open");
        }

        MarkDto mark = markQueryService.createMark(request.title(), request.description());
        for (UUID submissionId : suggestion.getMemberSubmissionIds()) {
            associate(submissionId, mark.id());
        }

        suggestion.setStatus(MarkCreationSuggestionStatus.ACTIONED);
        suggestion.setCreatedMarkId(mark.id());
        suggestion.setUpdatedAt(Instant.now());
        return toDto(suggestionRepository.save(suggestion));
    }

    @Transactional
    public MarkCreationSuggestionDto dismiss(UUID suggestionId) {
        MarkCreationSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Suggestion " + suggestionId + " not found"));
        suggestion.setStatus(MarkCreationSuggestionStatus.DISMISSED);
        suggestion.setUpdatedAt(Instant.now());
        return toDto(suggestionRepository.save(suggestion));
    }

    /**
     * Links one cluster member to the new mark: creates its occurrence from the
     * submission's coordinates, links the evidence, and rewrites the member's
     * decision as a positive label.
     */
    private void associate(UUID submissionId, UUID markId) {
        SubmissionRef submission = intakeApi.findById(submissionId).orElse(null);
        if (submission == null || submission.originalMediaFileId() == null) {
            return;
        }
        MarkOccurrenceDto occurrence = markQueryService.findOrCreateOccurrence(
                markId, null, submission.latitude(), submission.longitude());
        float[] embedding = processingApi.findEmbedding(submissionId).orElse(null);
        markQueryService.linkFileToOccurrence(
                submission.originalMediaFileId(), occurrence.id(), submissionId, embedding);

        reviewRepository.findBySubmissionId(submissionId).ifPresent(review -> {
            review.setDecision(ReviewDecision.APPROVED);
            review.setSelectedMarkId(markId);
            reviewRepository.save(review);
        });
    }

    private List<Cluster> cluster(List<SubmissionEmbedding> items) {
        boolean[] used = new boolean[items.size()];
        List<Cluster> clusters = new java.util.ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (used[i]) continue;
            SubmissionEmbedding seed = items.get(i);
            used[i] = true;
            Set<UUID> members = new LinkedHashSet<>();
            members.add(seed.submissionId());
            List<float[]> vectors = new java.util.ArrayList<>();
            vectors.add(seed.embedding());
            for (int j = i + 1; j < items.size(); j++) {
                if (used[j]) continue;
                if (cosineDistance(seed.embedding(), items.get(j).embedding()) <= maxDistance) {
                    used[j] = true;
                    members.add(items.get(j).submissionId());
                    vectors.add(items.get(j).embedding());
                }
            }
            clusters.add(new Cluster(members, coherence(vectors)));
        }
        return clusters;
    }

    private void reconcile(List<Cluster> clusters) {
        List<MarkCreationSuggestion> open = suggestionRepository.findByStatus(MarkCreationSuggestionStatus.OPEN);
        List<Set<UUID>> dismissed = suggestionRepository.findByStatus(MarkCreationSuggestionStatus.DISMISSED).stream()
                .map(suggestion -> Set.copyOf(suggestion.getMemberSubmissionIds()))
                .toList();

        Set<Set<UUID>> significant = new HashSet<>();
        for (Cluster cluster : clusters) {
            Set<UUID> members = Set.copyOf(cluster.members());
            significant.add(members);
            // A dismissed cluster stays dismissed while the same findings remain part of
            // it, even as the cluster grows with more of them.
            if (dismissed.stream().anyMatch(members::containsAll)) {
                continue;
            }
            Optional<MarkCreationSuggestion> existing = open.stream()
                    .filter(suggestion -> Set.copyOf(suggestion.getMemberSubmissionIds()).equals(members))
                    .findFirst();
            if (existing.isPresent()) {
                MarkCreationSuggestion suggestion = existing.get();
                suggestion.setSize(members.size());
                suggestion.setCoherence(cluster.coherence());
                suggestion.setUpdatedAt(Instant.now());
                suggestionRepository.save(suggestion);
            } else {
                suggestionRepository.save(MarkCreationSuggestion.builder()
                        .status(MarkCreationSuggestionStatus.OPEN)
                        .size(members.size())
                        .coherence(cluster.coherence())
                        .memberSubmissionIds(new LinkedHashSet<>(cluster.members()))
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build());
            }
        }

        open.stream()
                .filter(suggestion -> !significant.contains(Set.copyOf(suggestion.getMemberSubmissionIds())))
                .forEach(suggestionRepository::delete);
    }

    private static double coherence(List<float[]> vectors) {
        if (vectors.size() < 2) {
            return 1.0;
        }
        int dimension = vectors.getFirst().length;
        double[] centroid = new double[dimension];
        for (float[] vector : vectors) {
            for (int d = 0; d < dimension; d++) {
                centroid[d] += vector[d];
            }
        }
        for (int d = 0; d < dimension; d++) {
            centroid[d] /= vectors.size();
        }
        double sum = 0;
        for (float[] vector : vectors) {
            sum += cosineSimilarity(vector, centroid);
        }
        return sum / vectors.size();
    }

    private static double cosineDistance(float[] a, float[] b) {
        return 1.0 - cosineSimilarity(a, b);
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static double cosineSimilarity(float[] a, double[] b) {
        if (a == null || a.length != b.length || a.length == 0) {
            return 0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static MarkCreationSuggestionDto toDto(MarkCreationSuggestion suggestion) {
        return new MarkCreationSuggestionDto(
                suggestion.getId(),
                suggestion.getStatus(),
                suggestion.getSize(),
                suggestion.getCoherence(),
                suggestion.getMemberSubmissionIds(),
                suggestion.getCreatedMarkId(),
                suggestion.getCreatedAt(),
                suggestion.getUpdatedAt());
    }

    private record Cluster(Set<UUID> members, double coherence) {
    }
}
