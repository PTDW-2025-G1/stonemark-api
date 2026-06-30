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

    private static final String V2_SPECIAL = "\\_*[]()~`>#+-=|}{.!";

    /**
     * Temporary Unicode private-use markers that stand in for MarkdownV2 format chars
     * while we escape plain text. Never appears in message content, so safe to use as sentinels.
     */
    private static final char C_OPEN = '\uE000';
    private static final char C_CLOSE = '\uE001';
    private static final char B_OPEN = '\uE002';
    private static final char B_CLOSE = '\uE003';
    private static final char I_OPEN = '\uE004';
    private static final char I_CLOSE = '\uE005';

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
            return RenderedText.markdownV2(buildMarkdown(raw, args));
        }
        return RenderedText.plain(buildPlainText(raw, args));
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

    private String buildPlainText(String raw, Object[] args) {
        return fillPlaceholders(raw.replace("\\n", "\n"), args);
    }

    /**
     * Transforms a template like "{b}Hello{/b} {0}" into escaped MarkdownV2.
     * <p>
     * Strategy: replace {b}, {i}, {code} tags with temporary markers, then escape
     * plain text for V2 (code blocks get lighter escaping), then swap markers for
     * their real V2 equivalents: *, _, `.
     */
    private String buildMarkdown(String raw, Object[] args) {
        String text = raw.replace("\\n", "\n");
        text = replaceTags(text, CODE, C_OPEN, C_CLOSE);
        text = replaceTags(text, BOLD, B_OPEN, B_CLOSE);
        text = replaceTags(text, ITALIC, I_OPEN, I_CLOSE);
        text = fillPlaceholders(text, args);

        // Escape plain text between markers (code content gets special handling)
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == C_OPEN) {
                out.append(c);
                while (++i < text.length() && text.charAt(i) != C_CLOSE) {
                    char in = text.charAt(i);
                    if (in == '\\') out.append("\\\\"); // code blocks: only escape \ and `
                    else if (in == '`') out.append("\\`");
                    else out.append(in);
                }
                if (i < text.length()) out.append(text.charAt(i));
            } else if (c == B_OPEN || c == B_CLOSE || c == I_OPEN || c == I_CLOSE) {
                out.append(c); // pass format markers through unchanged
            } else if (V2_SPECIAL.indexOf(c) >= 0) {
                out.append('\\').append(c); // escape V2 reserved chars in plain text
            } else {
                out.append(c);
            }
        }

        text = out.toString();
        text = replaceMarkers(text, C_OPEN, C_CLOSE, '`');
        text = replaceMarkers(text, B_OPEN, B_CLOSE, '*');
        text = replaceMarkers(text, I_OPEN, I_CLOSE, '_');
        return text;
    }

    private String replaceTags(String input, Pattern pattern, char open, char close) {
        Matcher m = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, open + Matcher.quoteReplacement(m.group(1)) + close);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String fillPlaceholders(String input, Object[] args) {
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

    private static String replaceMarkers(String text, char open, char close, char real) {
        return text.replace(String.valueOf(open), String.valueOf(real))
                .replace(String.valueOf(close), String.valueOf(real));
    }
}
