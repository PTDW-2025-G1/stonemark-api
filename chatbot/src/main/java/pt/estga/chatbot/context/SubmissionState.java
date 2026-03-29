package pt.estga.chatbot.context;

public enum SubmissionState implements ConversationState {
    SUBMISSION_STATE,
    WAITING_FOR_PHOTO,
    AWAITING_LOCATION,
    AWAITING_NOTES,
    SUBMITTED
}
