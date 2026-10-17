package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.api.IntakeSubmissionApi;
import pt.estga.intake.api.SubmissionRef;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IntakeSubmissionApiImpl implements IntakeSubmissionApi {

    private final MarkEvidenceSubmissionRepository submissionRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<SubmissionRef> findById(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .map(s -> new SubmissionRef(s.getId(), s.getOriginalMediaFileId()));
    }

    @Override
    @Transactional
    public void markProcessed(Long submissionId) {
        updateStatus(submissionId, SubmissionStatus.PROCESSED);
    }

    @Override
    @Transactional
    public void markRejected(Long submissionId) {
        updateStatus(submissionId, SubmissionStatus.REJECTED);
    }

    private void updateStatus(Long submissionId, SubmissionStatus target) {
        submissionRepository.findById(submissionId).ifPresent(submission -> {
            if (submission.getStatus() == target) {
                return;
            }
            if (target == SubmissionStatus.PROCESSED) {
                submission.markProcessed();
            } else {
                submission.markRejected();
            }
            submissionRepository.save(submission);
        });
    }
}
