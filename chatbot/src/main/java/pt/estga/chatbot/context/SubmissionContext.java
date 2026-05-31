package pt.estga.chatbot.context;

import lombok.Getter;
import lombok.Setter;
import pt.estga.intake.enums.SubmissionSource;

import java.util.UUID;

@Getter
@Setter
public class SubmissionContext {
    private UUID stagedFileId;
    private String photoFilename;
    private SubmissionSource submissionSource;

    public void clear() {
        this.stagedFileId = null;
        this.photoFilename = null;
        this.submissionSource = null;
    }
}
