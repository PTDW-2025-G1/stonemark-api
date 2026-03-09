package pt.estga.chatbot.constants;

public class MessageKey {

    // General
    public static final String WELCOME = "welcome";
    public static final String WELCOME_BACK = "welcome_back";
    public static final String ERROR_GENERIC = "error_generic";
    public static final String ERROR_PROCESSING_PHOTO = "error_processing_photo";
    public static final String INVALID_SELECTION = "invalid_selection";
    public static final String HELP_OPTIONS_TITLE = "help_options_title";

    // Submission Flow
    public static final String PROPOSE_MARK_BTN = "propose_mark_btn";
    
    public static final String REQUEST_PHOTO_PROMPT = "request_photo_prompt";
    public static final String EXPECTING_PHOTO_ERROR = "expecting_photo_error";
    
    public static final String REQUEST_LOCATION_PROMPT = "request_location_prompt";
    public static final String EXPECTING_LOCATION_ERROR = "expecting_location_error";

    public static final String SKIP_BTN = "skip_btn";

    public static final String ADD_NOTES_PROMPT = "add_notes_prompt";
    public static final String SUBMISSION_SUCCESS = "submission_success";

    public static final String YES_BTN = "yes_btn";
    public static final String NO_BTN = "no_btn";

    // Authentication & Verification
    public static final String CONNECT_ACCOUNT_BTN = "connect_account_btn";
    public static final String MESSENGER_CONNECT_SUCCESS = "messenger_connect_success";
    public static final String USER_NOT_FOUND_ERROR = "user_not_found_error";

    public static final String CONNECT_MESSENGER_INSTRUCTIONS = "connect_messenger_instructions";
    public static final String CONNECT_MESSENGER_CODE = "connect_messenger_code";

    public static final String ACCOUNT_CONNECTED_NOTIFICATION = "account_connected_notification";

    private MessageKey() {}
}
