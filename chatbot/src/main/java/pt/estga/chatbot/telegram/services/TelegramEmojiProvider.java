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
            case TRASH -> "🗑️";
            case SEARCH -> "🔍";
            case NEW -> "🆕";
            case MEMO -> "📝";
            case MONUMENT -> "🏛️";
            case REFRESH -> "🔄";
            case BACK -> "🔙";
            case TADA -> "🎉";
            case CHECK -> "✅";
            case CROSS -> "❌";
            case LOCK -> "🔒";
            case KEY -> "🔑";
            case PHONE -> "📱";
            case NUMBERS -> "🔢";
            case ARROW_RIGHT -> "➡️";
            case THINKING -> "🤔";
            case HOURGLASS -> "⏳";
        };
    }
}
