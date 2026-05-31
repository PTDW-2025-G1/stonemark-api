package pt.estga.chatbot.constants;

import static pt.estga.chatbot.constants.EmojiKey.*;

public enum MessageKey {

    // General
    WELCOME("welcome", WAVE),
    WELCOME_BACK("welcome_back", WAVE),
    ERROR_GENERIC("error_generic", WARNING),
    TOO_MANY_ATTEMPTS("too_many_attempts", WARNING),
    HELP_OPTIONS_TITLE("help_options_title"),

    // Submission Flow
    PROPOSE_MARK_BTN("propose_mark_btn"),
    REQUEST_PHOTO_PROMPT("request_photo_prompt", CAMERA),
    EXPECTING_PHOTO_ERROR("expecting_photo_error", WARNING),
    REQUEST_LOCATION_PROMPT("request_location_prompt", LOCATION, PAPERCLIP),
    EXPECTING_LOCATION_ERROR("expecting_location_error", WARNING),
    SKIP_BTN("skip_btn", ARROW_RIGHT),
    ADD_NOTES_PROMPT("add_notes_prompt", MEMO),
    SUBMISSION_SUCCESS("submission_success", TADA),

    // Authentication & Verification
    CONNECT_ACCOUNT_BTN("connect_account_btn", KEY),
    MESSENGER_CONNECT_SUCCESS("messenger_connect_success", CHECK),
    USER_NOT_FOUND_ERROR("user_not_found_error", WARNING),
    CONNECT_MESSENGER_INSTRUCTIONS("connect_messenger_instructions", KEY),
    CONNECT_MESSENGER_CODE("connect_messenger_code"),
    ACCOUNT_CONNECTED_NOTIFICATION("account_connected_notification", TADA);

    private final String key;
    private final EmojiKey[] defaultEmojis;

    MessageKey(String key, EmojiKey... defaultEmojis) {
        this.key = key;
        this.defaultEmojis = defaultEmojis;
    }

    public String getKey() {
        return key;
    }

    public EmojiKey[] getDefaultEmojis() {
        return defaultEmojis;
    }
}
