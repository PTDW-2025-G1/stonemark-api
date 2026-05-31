package pt.estga.chatbot.models.ui;

import lombok.Builder;
import lombok.Data;
import pt.estga.chatbot.models.text.RichText;

import java.util.List;

@Data
@Builder
public class Menu implements UIComponent {
    private RichText titleNode;
    private List<List<Button>> buttons;
}
