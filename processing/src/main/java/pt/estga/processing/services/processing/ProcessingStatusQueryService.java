package pt.estga.processing.services.processing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.dtos.ProcessingStatusDto;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcessingStatusQueryService {

    private final MarkEvidenceProcessingRepository processingRepository;

    public Optional<ProcessingStatusDto> findStatusBySubmissionId(Long submissionId) {
        return processingRepository.findBySubmissionId(submissionId)
                .map(this::toDto);
    }

    private ProcessingStatusDto toDto(MarkEvidenceProcessing p) {
        return new ProcessingStatusDto(
                p.getId(),
                p.getSubmissionId(),
                p.getStatus(),
                p.getProcessedAt(),
                p.getFailedAt(),
                p.getLastRetryAt(),
                p.getRetryCount(),
                p.getMaxRetries(),
                p.isPermanent(),
                p.getErrorMessage()
        );
    }
}
