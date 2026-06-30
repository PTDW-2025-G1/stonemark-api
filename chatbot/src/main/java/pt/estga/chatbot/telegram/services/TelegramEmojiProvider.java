package pt.estga.chatbot.telegram.services;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.EmojiKey;

@Component
public class TelegramEmojiProvider {

    public String render(EmojiKey key) {
        return switch (key) {
            case WAVE -> "👋";
            case WARNING -> "⚠️";
            case CAMERA -> "📷";
            case LOCATION -> "📍";
            case PAPERCLIP -> "📎";
            case MEMO -> "📝";
            case TADA -> "🎉";
            case CHECK -> "✅";
            case KEY -> "🔑";
            case ARROW_RIGHT -> "➡️";
        };
    }
}
