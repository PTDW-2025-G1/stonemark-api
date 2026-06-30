package pt.estga.chatbot.models.ui;

import lombok.Builder;
import lombok.Data;
import pt.estga.chatbot.models.text.RenderedText;

@Data
@Builder
public final class LocationRequest implements UIComponent {
    private RenderedText messageNode;
}
