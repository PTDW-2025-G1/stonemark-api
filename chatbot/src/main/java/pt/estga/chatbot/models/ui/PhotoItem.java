package pt.estga.chatbot.models.ui;

import lombok.Builder;
import lombok.Data;
import pt.estga.chatbot.models.text.RenderedText;

import java.util.UUID;

@Data
@Builder
public final class PhotoItem implements UIComponent {
    private UUID mediaFileId;
    private RenderedText captionNode;
    private String callbackData;
}
