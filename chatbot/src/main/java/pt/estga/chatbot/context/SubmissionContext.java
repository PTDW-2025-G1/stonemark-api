package pt.estga.chatbot.context;

import lombok.Data;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionSource;

import java.util.List;

@Data
public class SubmissionContext {
    private MarkEvidenceSubmission submission;

    // Temporary data stored during chatbot flow (only submitted at the end)
    private byte[] photoData;
    private String photoFilename;
    private SubmissionSource submissionSource;

    public void clear() {
        this.submission = null;
        this.photoData = null;
        this.photoFilename = null;
        this.submissionSource = null;
    }
}
