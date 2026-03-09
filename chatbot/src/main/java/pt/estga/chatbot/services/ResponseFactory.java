package pt.estga.chatbot.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.ProposalState;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.Message;
import pt.estga.chatbot.models.ui.Menu;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static pt.estga.chatbot.constants.EmojiKey.WARNING;

@Service
@RequiredArgsConstructor
public class ResponseFactory {

    private final List<ResponseProvider> responseProviders;
    private final UiTextService textService;

    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
        ConversationState currentState = context.getCurrentState();

        if (outcome == HandlerOutcome.FAILURE) {
            return createErrorResponse(context);
        }

        for (ResponseProvider provider : responseProviders) {
            if (provider.supports(currentState)) {
                return provider.createResponse(context, outcome, input);
            }
        }

        return createErrorResponse(context);
    }

    public List<BotResponse> createErrorResponse(ChatbotContext context) {
        Message message = getFailureMessageForState(context.getCurrentState());
        return buildSimpleMenuResponse(message);
    }

    private List<BotResponse> buildSimpleMenuResponse(Message message) {
        if (message == null) {
            return Collections.singletonList(BotResponse.builder().textNode(textService.get(new Message(MessageKey.ERROR_GENERIC, WARNING))).build());
        }
        return Collections.singletonList(BotResponse.builder()
                .uiComponent(Menu.builder().titleNode(textService.get(message)).build())
                .build());
    }

    private Message getFailureMessageForState(ConversationState state) {
        if (state instanceof ProposalState proposalState) {
            return switch (proposalState) {
                case WAITING_FOR_PHOTO -> new Message(MessageKey.EXPECTING_PHOTO_ERROR, WARNING);
                case AWAITING_LOCATION -> new Message(MessageKey.EXPECTING_LOCATION_ERROR, WARNING);
                default -> null;
            };
        } else if (state instanceof VerificationState verificationState) {
            return switch (verificationState) {
                case DISPLAYING_VERIFICATION_CODE -> new Message(MessageKey.ERROR_GENERIC, WARNING);
                case AWAITING_CONTACT -> new Message(MessageKey.USER_NOT_FOUND_ERROR, WARNING);
                default -> null;
            };
        }
        return null;
    }
}
