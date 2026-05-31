package pt.estga.chatbot.context;

public sealed interface HandlerOutcome
        permits HandlerOutcome.Success,
                HandlerOutcome.Failure,
                HandlerOutcome.Redispatch {

    record Success() implements HandlerOutcome {}

    record Failure() implements HandlerOutcome {}

    record Redispatch() implements HandlerOutcome {}
}
