package pt.estga.chatbot.whatsapp;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.EmojiKey;
import pt.estga.chatbot.services.messages.EmojiProvider;

@Component("whatsappEmojiProvider")
public class WhatsAppEmojiProvider implements EmojiProvider {

    @Override
    public String render(EmojiKey key) {
        // WhatsApp supports standard unicode emojis, similar to Telegram
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
            default -> "❓";
        };
    }
}
