package pt.estga.chatbot.services.messages;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.constants.EmojiKey;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.models.Message;
import pt.estga.chatbot.models.text.RenderedText;
import pt.estga.chatbot.telegram.services.TelegramEmojiProvider;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UiTextService {

    private static final Pattern BOLD = Pattern.compile("\\{b}(.*?)\\{/b}");
    private static final Pattern ITALIC = Pattern.compile("\\{i}(.*?)\\{/i}");
    private static final Pattern CODE = Pattern.compile("\\{code}(.*?)\\{/code}");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d)}");

    private final MessageSource messageSource;
    private final TelegramEmojiProvider emojiProvider;

    public RenderedText get(MessageKey messageKey, Object... userArgs) {
        return get(messageKey.getKey(), mergeArgs(userArgs, messageKey.getDefaultEmojis()));
    }

    public RenderedText get(MessageKey messageKey) {
        return get(messageKey.getKey(), (Object[]) messageKey.getDefaultEmojis());
    }

    public RenderedText get(Message message) {
        return get(message.getKey(), message.getArgs());
    }

    public RenderedText get(String key) {
        return get(key, (Object[]) null);
    }

    public RenderedText get(String key, Object... args) {
        String raw = messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
        String processed = processTemplate(raw, args);
        return RenderedText.markdownV2(processed);
    }

    public String raw(String key) {
        return raw(key, (Object[]) null);
    }

    public String raw(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(key, null, locale);
        if (args != null && args.length > 0) {
            return MessageFormat.format(message, args);
        }
        return message;
    }

    private Object[] mergeArgs(Object[] userArgs, EmojiKey[] defaultEmojis) {
        if (defaultEmojis.length == 0) {
            return userArgs != null ? userArgs : new Object[0];
        }
        Object[] result = new Object[(userArgs != null ? userArgs.length : 0) + defaultEmojis.length];
        if (userArgs != null) {
            System.arraycopy(userArgs, 0, result, 0, userArgs.length);
        }
        System.arraycopy(defaultEmojis, 0, result, userArgs != null ? userArgs.length : 0, defaultEmojis.length);
        return result;
    }

    private String processTemplate(String raw, Object[] args) {
        String result = raw;
        result = result.replace("\\n", "\n");
        result = replaceTags(result, CODE, "``", "`");
        result = replaceTags(result, BOLD, "__", "*");
        result = replaceTags(result, ITALIC, "//", "_");
        result = replacePlaceholders(result, args);
        result = result.replace("``", "`");
        result = result.replace("__", "*");
        result = result.replace("//", "_");
        return result;
    }

    private String replaceTags(String input, Pattern pattern, String tempOpen, String realTag) {
        Matcher m = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        String open = tempOpen.isEmpty() ? realTag : tempOpen;
        while (m.find()) {
            m.appendReplacement(sb, open + Matcher.quoteReplacement(m.group(1)) + realTag);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String replacePlaceholders(String input, Object[] args) {
        if (args == null || args.length == 0) return input;
        Matcher m = PLACEHOLDER.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int idx = Integer.parseInt(m.group(1));
            Object replacement = idx < args.length ? args[idx] : "{missing}";
            String text = replacement instanceof EmojiKey ek ? emojiProvider.render(ek) : replacement.toString();
            m.appendReplacement(sb, Matcher.quoteReplacement(text));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
