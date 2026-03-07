package pt.estga.chatbot.context;

public enum VerificationState implements ConversationState {
    DISPLAYING_VERIFICATION_CODE,
    AWAITING_CONTACT,
    AWAITING_PHONE_CONNECTION_DECISION,
    PHONE_VERIFICATION_SUCCESS,
    PHONE_CONNECTION_SUCCESS
}
