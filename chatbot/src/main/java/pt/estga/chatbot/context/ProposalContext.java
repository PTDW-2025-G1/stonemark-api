package pt.estga.chatbot.context;

import lombok.Data;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionSource;

import java.util.List;

@Data
public class ProposalContext {
    private MarkEvidenceSubmission submission;
    private List<String> suggestedMarkIds;
    private List<String> suggestedMonumentIds;

    // Temporary data stored during chatbot flow (only submitted at the end)
    private byte[] photoData;
    private String photoFilename;
    private SubmissionSource submissionSource;

    public void clear() {
        this.submission = null;
        this.suggestedMarkIds = null;
        this.suggestedMonumentIds = null;
        this.photoData = null;
        this.photoFilename = null;
        this.submissionSource = null;
    }
}
