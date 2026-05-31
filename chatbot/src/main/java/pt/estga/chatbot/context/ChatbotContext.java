package pt.estga.chatbot.context;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotContext {
    private ConversationState currentState;
    private Long domainUserId;
    private String userName;
    private String verificationCode;
    private SubmissionContext submissionContext;
    private int consecutiveFailures;

    public ChatbotContext() {
        this.submissionContext = new SubmissionContext();
    }

    public void clear() {
        this.submissionContext.clear();
        this.consecutiveFailures = 0;
    }
}
