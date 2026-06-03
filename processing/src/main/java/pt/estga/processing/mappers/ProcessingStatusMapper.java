package pt.estga.processing.mappers;

import pt.estga.processing.dtos.ProcessingStatusDto;
import pt.estga.processing.entities.MarkEvidenceProcessing;

public class ProcessingStatusMapper {

    private ProcessingStatusMapper() {}

    public static ProcessingStatusDto toDto(MarkEvidenceProcessing p) {
        if (p == null) return null;
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
