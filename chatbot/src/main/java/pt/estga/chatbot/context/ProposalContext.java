package pt.estga.chatbot.context;

import lombok.Data;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.enums.SubmissionSource;

import java.util.List;

@Data
public class ProposalContext {
    private Proposal proposal;
    private List<String> suggestedMarkIds;
    private List<String> suggestedMonumentIds;

    // Temporary data stored during chatbot flow (only submitted at the end)
    private byte[] photoData;
    private String photoFilename;
    private SubmissionSource submissionSource;

    public void clear() {
        this.proposal = null;
        this.suggestedMarkIds = null;
        this.suggestedMonumentIds = null;
        this.photoData = null;
        this.photoFilename = null;
        this.submissionSource = null;
    }
}
