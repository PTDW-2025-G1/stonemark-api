package pt.estga.chatbot.utils;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.models.text.RichText;
import pt.estga.chatbot.models.text.RichText.Bold;
import pt.estga.chatbot.models.text.RichText.Code;
import pt.estga.chatbot.models.text.RichText.Group;
import pt.estga.chatbot.models.text.RichText.Italic;
import pt.estga.chatbot.models.text.RichText.NewLine;
import pt.estga.chatbot.models.text.RichText.Plain;
import pt.estga.chatbot.models.text.RichText.Placeholder;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextTemplateParser {

    private static final String[] SPECIALS = {
            "\n",
            "{b}", "{/b}",
            "{i}", "{/i}",
            "{code}", "{/code}",
            "{0}", "{1}", "{2}", "{3}", "{4}", "{5}", "{6}", "{7}", "{8}", "{9}"
    };

    public RichText parse(String input) {
        return new Group(parseNodes(input));
    }

    private List<RichText> parseNodes(String input) {
        List<RichText> nodes = new ArrayList<>();
        int i = 0;

        while (i < input.length()) {
            if (input.startsWith("\n", i)) {
                nodes.add(NewLine.INSTANCE);
                i++;
                continue;
            }

            if (input.startsWith("{b}", i)) {
                int end = input.indexOf("{/b}", i);
                if (end == -1) throw new IllegalStateException("Unclosed {b} tag in message: " + input);
                nodes.add(new Bold(parseNodes(input.substring(i + 3, end))));
                i = end + 4;
                continue;
            }

            if (input.startsWith("{i}", i)) {
                int end = input.indexOf("{/i}", i);
                if (end == -1) throw new IllegalStateException("Unclosed {i} tag in message: " + input);
                nodes.add(new Italic(parseNodes(input.substring(i + 3, end))));
                i = end + 4;
                continue;
            }

            if (input.startsWith("{code}", i)) {
                int end = input.indexOf("{/code}", i);
                if (end == -1) throw new IllegalStateException("Unclosed {code} tag in message: " + input);
                nodes.add(new Code(input.substring(i + 6, end)));
                i = end + 7;
                continue;
            }

            if (input.startsWith("{", i) && input.indexOf("}", i) > i + 1) {
                int end = input.indexOf("}", i);
                String placeholder = input.substring(i + 1, end);
                try {
                    int idx = Integer.parseInt(placeholder);
                    nodes.add(new Placeholder(idx));
                    i = end + 1;
                    continue;
                } catch (NumberFormatException e) {
                }
            }

            int nextSpecial = findNextSpecial(input, i);
            if (nextSpecial == i) {
                int codePoint = input.codePointAt(i);
                int charCount = Character.charCount(codePoint);
                nodes.add(new Plain(input.substring(i, i + charCount)));
                i += charCount;
            } else {
                nodes.add(new Plain(input.substring(i, nextSpecial)));
                i = nextSpecial;
            }
        }

        return nodes;
    }

    private int findNextSpecial(String input, int start) {
        int next = input.length();
        for (String s : SPECIALS) {
            int idx = input.indexOf(s, start);
            if (idx != -1 && idx < next) {
                next = idx;
            }
        }
        return next;
    }
}
