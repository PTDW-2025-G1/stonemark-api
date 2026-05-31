package pt.estga.chatbot.features.core.commands;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.SharedCallbackData;
import pt.estga.chatbot.features.core.GlobalCommandHandler;
import pt.estga.chatbot.models.BotInput;

@Component
public class BackToMainMenuHandler implements GlobalCommandHandler {
    @Override
    public boolean matches(BotInput input) {
        return input.getCallbackData() != null && input.getCallbackData().equals(SharedCallbackData.BACK_TO_MAIN_MENU);
    }
}
