package pt.estga.chatbot.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.constants.EmojiKey;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.models.Message;
import pt.estga.chatbot.models.text.*;
import pt.estga.chatbot.utils.TextTemplateParser;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UiTextService {

    private final MessageSource messageSource;
    private final TextTemplateParser parser;

    public TextNode get(MessageKey messageKey, Object... userArgs) {
        return get(messageKey.getKey(), mergeArgs(userArgs, messageKey.getDefaultEmojis()));
    }

    public TextNode get(MessageKey messageKey) {
        return get(messageKey.getKey(), (Object[]) messageKey.getDefaultEmojis());
    }

    public TextNode get(Message message) {
        return get(message.getKey(), message.getArgs());
    }

    public TextNode get(String key) {
        return get(key, (Object[]) null);
    }

    public TextNode get(String key, Object... args) {
        String raw = messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
        TextNode ast = parser.parse(raw);
        if (args != null && args.length > 0) {
            ast = replacePlaceholders(ast, args);
        }
        return ast;
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

    private TextNode replacePlaceholders(TextNode node, Object[] args) {
        if (node instanceof Placeholder p) {
            Object arg = args[p.index()];
            if (arg instanceof EmojiKey emojiKey) {
                return new Emoji(emojiKey);
            }
            return new Plain(arg.toString());
        } else if (node instanceof Container c) {
            List<TextNode> children = c.children().stream()
                    .map(child -> replacePlaceholders(child, args))
                    .toList();
            return new Container(children);
        } else if (node instanceof Bold b) {
            List<TextNode> children = b.children().stream()
                    .map(child -> replacePlaceholders(child, args))
                    .toList();
            return new Bold(children);
        } else if (node instanceof Italic i) {
            List<TextNode> children = i.children().stream()
                    .map(child -> replacePlaceholders(child, args))
                    .toList();
            return new Italic(children);
        } else if (node instanceof Code code) {
            return code;
        } else {
            return node; // Plain, NewLine
        }
    }

}
