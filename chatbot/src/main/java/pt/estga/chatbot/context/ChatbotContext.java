package pt.estga.chatbot.context;

import lombok.Data;

@Data
public class ChatbotContext {
    private ConversationState currentState;
    private Long domainUserId;
    private String userName;
    private String verificationCode;
    private ProposalContext proposalContext;

    public ChatbotContext() {
        this.proposalContext = new ProposalContext();
    }

    public void clear() {
        this.proposalContext.clear();
    }
}
