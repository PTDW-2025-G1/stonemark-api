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
    private static final Pattern HAS_FORMATTING = Pattern.compile("\\{[/]?[binc]");
    private static final String ESCAPE_CHARS = "\\_*[]()~`>#+-=|}{.!";

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
        if (HAS_FORMATTING.matcher(raw).find()) {
            return RenderedText.markdownV2(processFormatted(raw, args));
        }
        return RenderedText.plain(processPlain(raw, args));
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

    private String processPlain(String raw, Object[] args) {
        return replacePlaceholders(raw.replace("\\n", "\n"), args);
    }

    private String processFormatted(String raw, Object[] args) {
        String result = raw.replace("\\n", "\n");
        result = replaceTags(result, CODE, "\u0002", "\u0003");
        result = replaceTags(result, BOLD, "\u0004", "\u0005");
        result = replaceTags(result, ITALIC, "\u0006", "\u0007");
        result = replacePlaceholders(result, args);
        result = escapeV2Content(result);
        result = result.replace("\u0002", "`");
        result = result.replace("\u0003", "`");
        result = result.replace("\u0004", "*");
        result = result.replace("\u0005", "*");
        result = result.replace("\u0006", "_");
        result = result.replace("\u0007", "_");
        return result;
    }

    private String escapeV2Content(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u0002') {
                sb.append(c);
                while (++i < text.length() && text.charAt(i) != '\u0003') {
                    char inner = text.charAt(i);
                    if (inner == '\\') sb.append("\\\\");
                    else if (inner == '`') sb.append("\\`");
                    else sb.append(inner);
                }
                if (i < text.length()) sb.append(text.charAt(i));
            } else if (c == '\u0004' || c == '\u0005' || c == '\u0006' || c == '\u0007') {
                sb.append(c);
            } else if (ESCAPE_CHARS.indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String replaceTags(String input, Pattern pattern, String tempOpen, String realTag) {
        Matcher m = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, tempOpen + Matcher.quoteReplacement(m.group(1)) + realTag);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String replacePlaceholders(String input, Object[] args) {
        if (args == null || args.length == 0) return input;
        Matcher m = PLACEHOLDER.matcher(input);
        StringBuilder sb = new StringBuilder();
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
