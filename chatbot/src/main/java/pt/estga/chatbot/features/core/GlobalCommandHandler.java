package pt.estga.chatbot.features.core;

import pt.estga.chatbot.models.BotInput;

@FunctionalInterface
public interface GlobalCommandHandler {
    boolean matches(BotInput input);
}
