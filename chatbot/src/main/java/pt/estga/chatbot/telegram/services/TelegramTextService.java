package pt.estga.chatbot.telegram.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.models.text.RenderedText;
import pt.estga.chatbot.models.text.RichText;
import pt.estga.chatbot.telegram.TelegramRenderer;

@Component
@RequiredArgsConstructor
public class TelegramTextService {

    private final TelegramRenderer renderer;

    public RenderedText render(RichText node) {
        return renderer.render(node);
    }
}
