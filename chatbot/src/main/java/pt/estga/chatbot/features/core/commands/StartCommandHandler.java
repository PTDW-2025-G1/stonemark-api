package pt.estga.chatbot.features.core.commands;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.features.core.GlobalCommandHandler;
import pt.estga.chatbot.models.BotInput;

@Component
public class StartCommandHandler implements GlobalCommandHandler {
    @Override
    public boolean matches(BotInput input) {
        return input.getText() != null && input.getText().startsWith("/start");
    }
}
