package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.api.ProcessingModerationView;
import pt.estga.processing.api.ProcessingOverview;
import pt.estga.processing.api.ProcessingReviewApi;
import pt.estga.processing.api.SubmissionEmbedding;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.mappers.MarkSuggestionMapper;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessingReviewApiImpl implements ProcessingReviewApi {

    private final MarkEvidenceProcessingRepository processingRepository;
    private final MarkSuggestionRepository suggestionRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessingOverview> findOverview(Long submissionId) {
        return processingRepository.findOverviewBySubmissionId(submissionId)
                .map(p -> new ProcessingOverview(p.getId(), p.getStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarkSuggestionDto> findSuggestions(UUID processingId) {
        return suggestionRepository.findByProcessingId(processingId).stream()
                .map(MarkSuggestionMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSuggestions(UUID processingId) {
        return suggestionRepository.countByProcessingId(processingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double maxSuggestionConfidence(UUID processingId) {
        return suggestionRepository.findMaxConfidenceByProcessingId(processingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<float[]> findEmbedding(Long submissionId) {
        return processingRepository.findEmbeddingTextBySubmissionId(submissionId).map(ProcessingEmbeddingParser::parse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionEmbedding> findEmbeddings(Collection<Long> submissionIds) {
        if (submissionIds == null || submissionIds.isEmpty()) {
            return List.of();
        }
        return processingRepository.findBySubmissionIdInAndEmbeddingIsNotNull(submissionIds).stream()
                .map(processing -> new SubmissionEmbedding(processing.getSubmissionId(), processing.getEmbedding()))
                .toList();
    }

    @Override
    @Transactional
    public void markReviewed(Long submissionId) {
        processingRepository.updateStatusBySubmissionId(submissionId, ProcessingStatus.REVIEWED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessingModerationView> findProcessingByConfidence(double min, double max) {
        return suggestionRepository.findProcessingByMaxConfidenceBetween(min, max).stream()
                .map(p -> new ProcessingModerationView(p.getProcessingId(), p.getSubmissionId(),
                        p.getStatus(), p.getMaxConfidence()))
                .toList();
    }
}
