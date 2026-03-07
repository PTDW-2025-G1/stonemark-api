package pt.estga.chatbot.constants;

public class MessageKey {

    // General
    public static final String WELCOME = "welcome";
    public static final String WELCOME_BACK = "welcome_back";
    public static final String ERROR_GENERIC = "error_generic";
    public static final String ERROR_PROCESSING_PHOTO = "error_processing_photo";
    public static final String INVALID_SELECTION = "invalid_selection";
    public static final String HELP_OPTIONS_TITLE = "help_options_title";

    // Proposal Flow
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
    public static final String AUTH_REQUIRED_TITLE = "auth_required_title";
    public static final String VERIFY_ACCOUNT_BTN = "verify_account_btn";
    public static final String SHARE_PHONE_NUMBER_PROMPT = "share_phone_number_prompt";
    public static final String VERIFICATION_SUCCESS_CODE = "verification_success_code";
    public static final String USER_NOT_FOUND_ERROR = "user_not_found_error";

    public static final String DISPLAY_VERIFICATION_CODE_TITLE = "display_verification_code_title";
    public static final String DISPLAY_VERIFICATION_CODE = "display_verification_code";

    public static final String VERIFICATION_SUCCESS_PHONE = "verification_success_phone";
    public static final String PROMPT_CONNECT_PHONE = "prompt_connect_phone";
    public static final String PHONE_CONNECTION_SUCCESS = "phone_connection_success";


    private MessageKey() {}
}
