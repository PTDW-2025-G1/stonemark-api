package pt.estga.chatbot.context;

import lombok.Data;

@Data
public class ChatbotContext {
    private ConversationState currentState;
    private Long domainUserId;
    private String userName;
    private String verificationCode;
    private SubmissionContext submissionContext;

    public ChatbotContext() {
        this.submissionContext = new SubmissionContext();
    }

    public void clear() {
        this.submissionContext.clear();
    }
}
