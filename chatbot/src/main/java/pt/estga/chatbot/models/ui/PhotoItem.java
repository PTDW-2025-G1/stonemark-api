package pt.estga.chatbot.models.ui;

import lombok.Builder;
import lombok.Data;
import pt.estga.chatbot.models.text.RichText;

import java.util.UUID;

@Data
@Builder
public final class PhotoItem implements UIComponent {
    private UUID mediaFileId;
    private RichText captionNode;
    private String callbackData;
}
