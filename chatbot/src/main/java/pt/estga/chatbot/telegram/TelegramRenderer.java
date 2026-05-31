package pt.estga.chatbot.telegram;

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

import java.util.List;

@Component
@Slf4j
public class TelegramRenderer implements TextRenderer {

    private final EmojiProvider emojiProvider;

    public TelegramRenderer(@Qualifier("telegramEmojiProvider") EmojiProvider emojiProvider) {
        this.emojiProvider = emojiProvider;
    }

    @Override
    public RenderedText render(RichText node) {
        boolean hasFormatting = containsFormatting(node);
        String text = renderNode(node, hasFormatting);

        String parseMode = hasFormatting ? "MarkdownV2" : null;
        return new RenderedText(text, parseMode);
    }

    private String renderNode(RichText node, boolean escapeContent) {
        return switch (node) {
            case Plain p -> escapeContent ? escape(p.text()) : p.text();
            case Bold b -> "*" + renderChildren(b.children(), escapeContent) + "*";
            case Italic i -> "_" + renderChildren(i.children(), escapeContent) + "_";
            case Code c -> "`" + escapeCode(c.text()) + "`";
            case Emoji e -> emojiProvider.render(e.key());
            case Placeholder p -> "{" + p.index() + "}";
            case NewLine ignored -> "\n";
            case Group c -> renderChildren(c.children(), escapeContent);
            default -> throw new IllegalStateException("Unexpected value: " + node);
        };
    }

    private boolean containsFormatting(RichText node) {
        if (node instanceof Bold || node instanceof Italic || node instanceof Code) {
            return true;
        } else if (node instanceof Group(List<RichText> children)) {
            for (RichText child : children) {
                if (containsFormatting(child)) return true;
            }
        }
        return false;
    }

    private String renderChildren(Iterable<RichText> children, boolean escapeContent) {
        StringBuilder sb = new StringBuilder();
        for (RichText child : children) {
            sb.append(renderNode(child, escapeContent));
        }
        return sb.toString();
    }

    private String escape(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (shouldEscape(c)) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private boolean shouldEscape(char c) {
        return c == '\\' || c == '_' || c == '*' || c == '[' || c == ']' || c == '(' || c == ')' || c == '~' || c == '`' || c == '>' || c == '#' || c == '+' || c == '-' || c == '=' || c == '|' || c == '{' || c == '}' || c == '.' || c == '!';
    }

    private String escapeCode(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("`", "\\`");
    }
}
