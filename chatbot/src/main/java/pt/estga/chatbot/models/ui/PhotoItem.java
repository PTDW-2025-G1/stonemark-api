package pt.estga.chatbot.models.ui;

import lombok.Builder;
import lombok.Data;
import pt.estga.chatbot.models.text.TextNode;

import java.util.UUID;

@Data
@Builder
public class PhotoItem implements UIComponent {
    private UUID mediaFileId;
    private TextNode captionNode;
    private String callbackData;
}
