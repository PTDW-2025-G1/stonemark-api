package pt.estga.chatbot.models.text;

public record RenderedText(String text, String parseMode) {

    public static RenderedText plain(String text) {
        return new RenderedText(escape(text), null);
    }

    public static RenderedText markdownV2(String text) {
        return new RenderedText(text, "MarkdownV2");
    }

    private static String escape(String text) {
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

    private static boolean shouldEscape(char c) {
        return c == '\\' || c == '_' || c == '*' || c == '[' || c == ']'
                || c == '(' || c == ')' || c == '~' || c == '`' || c == '>'
                || c == '#' || c == '+' || c == '-' || c == '='
                || c == '|' || c == '{' || c == '}' || c == '.' || c == '!';
    }
}
