package pt.estga.chatbot.services.messages;

import pt.estga.chatbot.models.text.RenderedText;
import pt.estga.chatbot.models.text.RichText;

public interface TextRenderer {
    RenderedText render(RichText node);
}
