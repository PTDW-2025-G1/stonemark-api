package pt.estga.chatbot.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import pt.estga.chatbot.models.text.RenderedText;
import pt.estga.chatbot.models.ui.Menu;
import pt.estga.chatbot.models.ui.UIComponent;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotResponse {
    private RenderedText textNode;
    private UIComponent uiComponent;

    public static List<BotResponse> menuResponse(RenderedText titleNode) {
        return Collections.singletonList(BotResponse.builder()
                .uiComponent(Menu.builder().titleNode(titleNode).build())
                .build());
    }
}
