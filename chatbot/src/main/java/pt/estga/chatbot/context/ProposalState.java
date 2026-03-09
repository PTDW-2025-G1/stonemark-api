package pt.estga.chatbot.context;

public enum ProposalState implements ConversationState {
    PROPOSAL_START,
    WAITING_FOR_PHOTO,
    AWAITING_LOCATION,
    AWAITING_PHOTO_ANALYSIS,
    AWAITING_MONUMENT_SUGGESTIONS,
    AWAITING_NOTES,
    SUBMITTED
}
