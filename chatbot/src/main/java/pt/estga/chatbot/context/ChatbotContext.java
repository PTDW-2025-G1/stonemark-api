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
    private long lastActivityTimestamp;

    public ChatbotContext() {
        this.submissionContext = new SubmissionContext();
        touch();
    }

    public void touch() {
        this.lastActivityTimestamp = System.currentTimeMillis();
    }

    public void clear() {
        this.submissionContext.clear();
    }
}
