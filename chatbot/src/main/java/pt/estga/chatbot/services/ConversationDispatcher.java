package pt.estga.chatbot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
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

        // Handle re-dispatch outcome
        if (outcome == HandlerOutcome.RE_DISPATCH) {
            log.debug("Re-dispatching due to RE_DISPATCH outcome");
            return dispatch(context, input, depth + 1);
        }

        // Determine the next state.
        ConversationState nextState = submissionFlow.getNextState(context, currentState, outcome);
        log.debug("State transition: {} -> {} (outcome: {})", currentState, nextState, outcome);
        ConversationState previousState = context.getCurrentState();
        context.setCurrentState(nextState);

        try {
            // If the next state is automatic, execute it first to populate context before generating response
            ConversationStateHandler nextHandler = handlers.get(nextState);
            if (nextHandler != null && nextHandler.isAutomatic()) {
                log.debug("Executing automatic handler: {} for state: {}", nextHandler.getClass().getSimpleName(), nextState);
                BotInput nextInput = BotInput.builder()
                        .userId(input.getUserId())
                        .platform(input.getPlatform())
                        .build();
                HandlerOutcome autoOutcome = nextHandler.handle(context, nextInput);
                log.debug("Automatic handler {} returned outcome: {}", nextHandler.getClass().getSimpleName(), autoOutcome);

                // If automatic handler needs state transition, handle it recursively
                if (autoOutcome != HandlerOutcome.SUCCESS && autoOutcome != HandlerOutcome.AWAITING_INPUT) {
                    ConversationState autoNextState = submissionFlow.getNextState(context, nextState, autoOutcome);
                    log.debug("Automatic state transition: {} -> {} (outcome: {})", nextState, autoNextState, autoOutcome);
                    context.setCurrentState(autoNextState);
                }
            }

            // Generate a response for the NEW state (after automatic handlers have populated context).
            return new ArrayList<>(responseFactory.createResponse(context, outcome, input));
        } catch (RuntimeException e) {
            log.error("Error generating response for state {}, rolling back to {}", nextState, previousState, e);
            context.setCurrentState(previousState);
            throw e;
        }
    }
}
