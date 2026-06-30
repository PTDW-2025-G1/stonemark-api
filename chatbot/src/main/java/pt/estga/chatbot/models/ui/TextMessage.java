package pt.estga.chatbot.models.ui;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import pt.estga.chatbot.models.text.RenderedText;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public final class TextMessage implements UIComponent {
    private RenderedText textNode;
}
