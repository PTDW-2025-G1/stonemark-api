package pt.estga.chatbot.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.models.text.RenderedText;
import pt.estga.chatbot.models.text.RichText;
import pt.estga.chatbot.models.text.RichText.Bold;
import pt.estga.chatbot.models.text.RichText.Code;
import pt.estga.chatbot.models.text.RichText.Emoji;
import pt.estga.chatbot.models.text.RichText.Group;
import pt.estga.chatbot.models.text.RichText.Italic;
import pt.estga.chatbot.models.text.RichText.NewLine;
import pt.estga.chatbot.models.text.RichText.Plain;
import pt.estga.chatbot.models.text.RichText.Placeholder;
import pt.estga.chatbot.services.messages.EmojiProvider;
import pt.estga.chatbot.services.messages.TextRenderer;

@Component
@Slf4j
public class WhatsAppTextRenderer implements TextRenderer {

    private final EmojiProvider emojiProvider;

    public WhatsAppTextRenderer(@Qualifier("whatsappEmojiProvider") EmojiProvider emojiProvider) {
        this.emojiProvider = emojiProvider;
    }

    @Override
    public RenderedText render(RichText node) {
        String text = renderNode(node);
        return new RenderedText(text, null);
    }

    private String renderNode(RichText node) {
        return switch (node) {
            case Plain p -> p.text();
            case Bold b -> "*" + renderChildren(b.children()) + "*";
            case Italic i -> "_" + renderChildren(i.children()) + "_";
            case Code c -> "```" + c.text() + "```";
            case Emoji e -> emojiProvider.render(e.key());
            case Placeholder p -> "{" + p.index() + "}";
            case NewLine ignored -> "\n";
            case Group c -> renderChildren(c.children());
        };
    }

    private String renderChildren(Iterable<RichText> children) {
        StringBuilder sb = new StringBuilder();
        for (RichText child : children) {
            sb.append(renderNode(child));
        }
        return sb.toString();
    }
}
