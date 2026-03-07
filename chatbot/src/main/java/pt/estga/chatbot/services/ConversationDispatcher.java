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

    private final Map<ConversationState, ConversationStateHandler> handlers;
    private final ConversationFlowManager proposalFlow;
    private final ResponseFactory responseFactory;

    public ConversationDispatcher(List<ConversationStateHandler> handlerList, ConversationFlowManager proposalFlow, ResponseFactory responseFactory) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(ConversationStateHandler::canHandle, Function.identity()));
        this.proposalFlow = proposalFlow;
        this.responseFactory = responseFactory;

        // Log all registered handlers for debugging
        log.info("Registering {} conversation state handlers", handlerList.size());
        handlerList.forEach(handler ->
            log.debug("Registered handler: {} for state: {}", handler.getClass().getSimpleName(), handler.canHandle())
        );
    }

    public List<BotResponse> dispatch(ChatbotContext context, BotInput input) {
        ConversationState currentState = context.getCurrentState();
        log.debug("Dispatching state: {} with input type: {}", currentState, input.getType());

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
            return dispatch(context, input);
        }

        // Determine the next state.
        ConversationState nextState = proposalFlow.getNextState(context, currentState, outcome);
        log.info("State transition: {} -> {} (outcome: {})", currentState, nextState, outcome);
        context.setCurrentState(nextState);

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
        }

        // Generate a response for the NEW state (after automatic handlers have populated context).
        List<BotResponse> responses = new ArrayList<>(responseFactory.createResponse(context, outcome, input));

        // If the next state is automatic and has a recursive outcome, continue dispatching
        if (nextHandler != null && nextHandler.isAutomatic()) {
            BotInput nextInput = BotInput.builder()
                    .userId(input.getUserId())
                    .platform(input.getPlatform())
                    .build();
            responses.addAll(dispatch(context, nextInput));
        }

        return responses;
    }
}
