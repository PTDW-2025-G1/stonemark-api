package pt.estga.chatbot.models;

import lombok.Builder;
import lombok.Data;
import pt.estga.sharedcore.models.Location;

import java.util.Locale;

@Data
@Builder
public class BotInput {
    private String userId;
    private long chatId;
    private Platform platform;
    private InputType type;
    private String text;
    private byte[] fileData;
    private String fileName;
    private Location location;
    private Locale locale;
    private String callbackData;

    public enum InputType {
        TEXT,
        COMMAND,
        PHOTO,
        LOCATION,
        CALLBACK,
        CONTACT
    }
}
