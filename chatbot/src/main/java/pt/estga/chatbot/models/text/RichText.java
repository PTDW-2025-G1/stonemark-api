package pt.estga.chatbot.models.text;

import pt.estga.chatbot.constants.EmojiKey;

import java.util.List;

public sealed interface RichText {

    record Plain(String text) implements RichText {}

    record Bold(List<RichText> children) implements RichText {}

    record Italic(List<RichText> children) implements RichText {}

    record Code(String text) implements RichText {}

    record Emoji(EmojiKey key) implements RichText {}

    record Placeholder(int index) implements RichText {}

    record Group(List<RichText> children) implements RichText {}

    final class NewLine implements RichText {
        public static final NewLine INSTANCE = new NewLine();
        private NewLine() {}
    }
}
