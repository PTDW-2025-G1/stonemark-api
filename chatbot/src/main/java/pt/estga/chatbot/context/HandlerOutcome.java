package pt.estga.chatbot.context;

/**
 * Represents the outcome of a ConversationStateHandler's execution.
 * This allows the central dispatcher to decide the next step in the conversation flow.
 */
public enum HandlerOutcome {
    SUCCESS,
    FAILURE,
    AWAITING_INPUT,
    CHANGE_LOCATION,
    CHANGE_PHOTO,
    CONTINUE,
    REJECTED,
    PROPOSE_NEW,
    DISCARD,
    DISCARD_CONFIRMED,
    START_NEW,
    START_VERIFICATION,
    VERIFY_WITH_CODE,
    RE_DISPATCH
}
