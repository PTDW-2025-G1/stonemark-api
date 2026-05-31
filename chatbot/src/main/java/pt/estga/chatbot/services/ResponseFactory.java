package pt.estga.chatbot.services;

import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.text.RichText;
import pt.estga.chatbot.models.ui.Menu;

import java.util.Collections;
import java.util.List;

public final class ResponseFactory {

    private ResponseFactory() {}

    public static List<BotResponse> menuResponse(RichText titleNode) {
        return Collections.singletonList(BotResponse.builder()
                .uiComponent(Menu.builder().titleNode(titleNode).build())
                .build());
    }
}
