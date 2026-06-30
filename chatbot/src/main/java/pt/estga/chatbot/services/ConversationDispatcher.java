package pt.estga.chatbot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.services.messages.UiTextService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConversationDispatcher {

    private static final int MAX_AUTO_STEPS = 10;
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private final Map<ConversationState, ConversationStateHandler> handlers;
    private final UiTextService textService;

    public ConversationDispatcher(
            List<ConversationStateHandler> handlerList,
            UiTextService textService
    ) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        ConversationStateHandler::canHandle,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(
                                    "Duplicate handler for state " + a.canHandle()
                            );
                        }
                ));
        this.textService = textService;
    }

    public List<BotResponse> dispatch(ChatbotContext context, BotInput input) {
        ConversationStateHandler handler = handlers.get(context.getCurrentState());
        if (handler == null) {
            log.warn("No handler found for state: {}", context.getCurrentState());
            return BotResponse.menuResponse(textService.get(MessageKey.ERROR_GENERIC));
        }

        HandlerOutcome outcome = handler.handle(context, input);

        if (outcome == HandlerOutcome.REDISPATCH) {
            context.setCurrentState(handler.getNextState(context, context.getCurrentState(), outcome, input));
            runAutomaticHandlers(context, input);
            ConversationStateHandler targetHandler = handlers.get(context.getCurrentState());
            if (targetHandler != null) {
                return targetHandler.createResponse(context, outcome, input);
            }
            return List.of();
        }

        if (outcome == HandlerOutcome.FAILURE) {
            int failures = context.getConsecutiveFailures() + 1;
            context.setConsecutiveFailures(failures);
            if (failures > MAX_CONSECUTIVE_FAILURES) {
                log.warn("Max consecutive failures ({}) reached", MAX_CONSECUTIVE_FAILURES);
                context.clear();
                context.setCurrentState(CoreState.START);
                return BotResponse.menuResponse(textService.get(MessageKey.TOO_MANY_ATTEMPTS));
            }
        } else {
            context.setConsecutiveFailures(0);
        }

        context.setCurrentState(handler.getNextState(context, context.getCurrentState(), outcome, input));

        runAutomaticHandlers(context, input);

        ConversationStateHandler responseHandler = handlers.get(context.getCurrentState());
        if (responseHandler != null) {
            return responseHandler.createResponse(context, outcome, input);
        }
        return List.of();
    }

    private void runAutomaticHandlers(ChatbotContext context, BotInput input) {
        for (int i = 0; i < MAX_AUTO_STEPS; i++) {
            ConversationStateHandler handler = handlers.get(context.getCurrentState());
            if (handler == null || !handler.isAutomatic()) break;

            BotInput autoInput = BotInput.builder()
                    .userId(input.getUserId())
                    .platform(input.getPlatform())
                    .build();
            HandlerOutcome outcome = handler.handle(context, autoInput);

            if (outcome == HandlerOutcome.SUCCESS) break;

            context.setCurrentState(handler.getNextState(context, context.getCurrentState(), outcome, autoInput));
        }
    }
}
