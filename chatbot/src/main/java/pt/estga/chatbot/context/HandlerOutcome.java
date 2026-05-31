package pt.estga.chatbot.context;

public sealed interface HandlerOutcome
        permits HandlerOutcome.Success,
                HandlerOutcome.Failure,
                HandlerOutcome.AwaitingInput,
                HandlerOutcome.Redispatch,
                HandlerOutcome.StartNew,
                HandlerOutcome.StartVerification {

    record Success() implements HandlerOutcome {}

    record Failure() implements HandlerOutcome {}

    record AwaitingInput() implements HandlerOutcome {}

    record Redispatch() implements HandlerOutcome {}

    record StartNew() implements HandlerOutcome {}

    record StartVerification() implements HandlerOutcome {}
}
