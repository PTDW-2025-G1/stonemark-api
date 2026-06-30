package pt.estga.chatbot.models.ui;

import lombok.Builder;
import lombok.Data;
import pt.estga.chatbot.models.text.RenderedText;

import java.util.List;

@Data
@Builder
public final class Menu implements UIComponent {
    private RenderedText titleNode;
    private List<List<Button>> buttons;
}
