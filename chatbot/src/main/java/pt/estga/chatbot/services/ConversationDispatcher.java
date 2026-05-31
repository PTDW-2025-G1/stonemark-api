package pt.estga.chatbot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.HandlerOutcome.AwaitingInput;
import pt.estga.chatbot.context.HandlerOutcome.Redispatch;
import pt.estga.chatbot.context.HandlerOutcome.Success;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConversationDispatcher {

    private static final int MAX_DISPATCH_DEPTH = 10;

    private final Map<ConversationState, ConversationStateHandler> handlers;
    private final ConversationFlowManager submissionFlow;
    private final ResponseFactory responseFactory;

    public ConversationDispatcher(
            List<ConversationStateHandler> handlerList,
            ConversationFlowManager submissionFlow,
            ResponseFactory responseFactory
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
        this.submissionFlow = submissionFlow;
        this.responseFactory = responseFactory;
    }

    public List<BotResponse> dispatch(ChatbotContext context, BotInput input) {
        return dispatch(context, input, 0);
    }

    private List<BotResponse> dispatch(ChatbotContext context, BotInput input, int depth) {
        if (depth > MAX_DISPATCH_DEPTH) {
            log.error("Max dispatch depth ({}) exceeded for state: {}", MAX_DISPATCH_DEPTH, context.getCurrentState());
            return responseFactory.createErrorResponse(context);
        }

        ConversationState currentState = context.getCurrentState();
        log.debug("Dispatching state: {} with input type: {} (depth: {})", currentState, input.getType(), depth);

        ConversationStateHandler handler = handlers.get(currentState);

        if (handler == null) {
            log.warn("No handler found for state: {}", currentState);
            return responseFactory.createErrorResponse(context);
        }

        log.debug("Executing handler: {} for state: {}", handler.getClass().getSimpleName(), currentState);

        // Execute the handler for the current state.
        HandlerOutcome outcome = handler.handle(context, input);

        log.debug("Handler {} returned outcome: {}", handler.getClass().getSimpleName(), outcome);

        if (outcome instanceof Redispatch) {
            log.debug("Re-dispatching due to RE_DISPATCH outcome");
            return dispatch(context, input, depth + 1);
        }

        ConversationState nextState = submissionFlow.getNextState(context, currentState, outcome);
        log.debug("State transition: {} -> {} (outcome: {})", currentState, nextState, outcome);
        ConversationState previousState = context.getCurrentState();
        context.setCurrentState(nextState);

        try {
            executeAutomaticHandlers(context, input);
            return new ArrayList<>(responseFactory.createResponse(context, outcome, input));
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

            log.debug("Executing automatic handler: {} for state: {}", handler.getClass().getSimpleName(), context.getCurrentState());
            BotInput autoInput = BotInput.builder()
                    .userId(input.getUserId())
                    .platform(input.getPlatform())
                    .build();
            HandlerOutcome outcome = handler.handle(context, autoInput);
            log.debug("Automatic handler {} returned outcome: {}", handler.getClass().getSimpleName(), outcome);

            if (outcome instanceof Success || outcome instanceof AwaitingInput) break;

            ConversationState nextState = submissionFlow.getNextState(context, context.getCurrentState(), outcome);
            log.debug("Automatic state transition: {} -> {} (outcome: {})", context.getCurrentState(), nextState, outcome);
            context.setCurrentState(nextState);
        }
    }
}
