package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionSource;
import pt.estga.user.services.UserQueryService;

import java.io.IOException;

/**
 * Thin facade that encapsulates chatbot-specific submission orchestration:
 * - resolves an optional domain user id to a User and attaches it to the submission
 * - ensures a submission source is set
 * - delegates the actual persistence and media saving to the application MarkEvidenceSubmissionSubmitService
 */
@Component
@RequiredArgsConstructor
public class ChatbotSubmissionFacade {

    private final MarkEvidenceSubmissionSubmitService markEvidenceSubmissionSubmitService;
    private final UserQueryService userQueryService;

    public void submitFromChatbot(
            MarkEvidenceSubmission submission,
            byte[] photoData,
            String photoFilename,
            Long domainUserId,
            SubmissionSource source
    ) throws IOException {
        if (domainUserId != null) {
            userQueryService.findById(domainUserId).ifPresent(submission::setSubmittedBy);
        }

        if (submission.getSubmissionSource() == null) {
            submission.setSubmissionSource(source != null ? source : SubmissionSource.OTHER);
        }

        markEvidenceSubmissionSubmitService.submit(submission, photoData, photoFilename);
    }
}
