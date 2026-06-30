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
import pt.estga.chatbot.models.text.RichText;
import pt.estga.chatbot.models.ui.Menu;
import pt.estga.chatbot.services.messages.UiTextService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConversationDispatcher {

    private static final int MAX_DISPATCH_DEPTH = 10;
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private final Map<ConversationState, ConversationStateHandler> handlers;
    private final UiTextService textService;

    public ConversationDispatcher(
            List<ConversationStateHandler> handlerList,
            UiTextService textService
    ) {
        this.handlers = handlerList.stream()
                .peek(h -> {
                    if (h.canHandle() == null) {
                        throw new IllegalStateException(
                                "Handler " + h.getClass().getName() + " returned null from canHandle()"
                        );
                    }
                })
                .collect(Collectors.toMap(
                        ConversationStateHandler::canHandle,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(
                                    "Duplicate handler registration for state " + a.canHandle()
                                            + ": " + a.getClass().getName() + " and " + b.getClass().getName()
                            );
                        }
                ));
        this.textService = textService;
    }

    public List<BotResponse> dispatch(ChatbotContext context, BotInput input) {
        return dispatch(context, input, 0);
    }

    private List<BotResponse> dispatch(ChatbotContext context, BotInput input, int depth) {
        if (depth > MAX_DISPATCH_DEPTH) {
            log.error("Max dispatch depth ({}) exceeded for state: {}", MAX_DISPATCH_DEPTH, context.getCurrentState());
            return createErrorResponse(context);
        }

        ConversationState currentState = context.getCurrentState();

        ConversationStateHandler handler = handlers.get(currentState);

        if (handler == null) {
            log.warn("No handler found for state: {}", currentState);
            return createErrorResponse(context);
        }

        HandlerOutcome outcome = handler.handle(context, input);

        if (outcome == HandlerOutcome.REDISPATCH) {
            return dispatch(context, input, depth + 1);
        }

        if (outcome == HandlerOutcome.FAILURE) {
            int failures = context.getConsecutiveFailures() + 1;
            context.setConsecutiveFailures(failures);
            if (failures > MAX_CONSECUTIVE_FAILURES) {
                log.warn("Max consecutive failures ({}) reached in state {}", MAX_CONSECUTIVE_FAILURES, currentState);
                context.clear();
                context.setCurrentState(CoreState.START);
                return Collections.singletonList(BotResponse.builder()
                        .uiComponent(Menu.builder()
                                .titleNode(textService.get(MessageKey.TOO_MANY_ATTEMPTS))
                                .build())
                        .build());
            }
        } else {
            context.setConsecutiveFailures(0);
        }

        ConversationState nextState = handler.getNextState(context, currentState, outcome, input);
        ConversationState previousState = context.getCurrentState();
        context.setCurrentState(nextState);

        try {
            executeAutomaticHandlers(context, input);
            ConversationStateHandler responseHandler = handlers.get(context.getCurrentState());
            if (responseHandler != null) {
                return new ArrayList<>(responseHandler.createResponse(context, outcome, input));
            }
            return Collections.emptyList();
        } catch (RuntimeException e) {
            log.error("Error generating response for state {}, rolling back to {}", nextState, previousState, e);
            context.setCurrentState(previousState);
            throw e;
        }
    }

    private void executeAutomaticHandlers(ChatbotContext context, BotInput input) {
        while (true) {
            ConversationStateHandler handler = handlers.get(context.getCurrentState());
            if (handler == null || !handler.isAutomatic()) break;

            BotInput autoInput = BotInput.builder()
                    .userId(input.getUserId())
                    .platform(input.getPlatform())
                    .build();
            HandlerOutcome outcome = handler.handle(context, autoInput);

            if (outcome == HandlerOutcome.SUCCESS) break;

            ConversationState nextState = handler.getNextState(context, context.getCurrentState(), outcome, autoInput);
            context.setCurrentState(nextState);
        }
    }

    private List<BotResponse> createErrorResponse(ChatbotContext context) {
        ConversationStateHandler handler = handlers.get(context.getCurrentState());
        if (handler != null) {
            RichText message = handler.failureResponse(context);
            if (message != null) {
                return BotResponse.menuResponse(message);
            }
        }
        return BotResponse.menuResponse(textService.get(MessageKey.ERROR_GENERIC));
    }
}
