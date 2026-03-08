package pt.estga.chatbot.features.proposal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.ProposalState;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubmissionFlowStrategyTest {

    private ProposalFlowStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ProposalFlowStrategy();
    }

    @Test
    void getNextState_ShouldAdvanceFromStartToPhoto_WhenSuccess() {
        ConversationState nextState = strategy.getNextState(
                new ChatbotContext(),
                ProposalState.PROPOSAL_START,
                HandlerOutcome.SUCCESS
        );

        assertEquals(ProposalState.WAITING_FOR_PHOTO, nextState);
    }

    @Test
    void getNextState_ShouldAdvanceFromPhotoToLocation_WhenSuccess() {
        ConversationState nextState = strategy.getNextState(
                new ChatbotContext(),
                ProposalState.WAITING_FOR_PHOTO,
                HandlerOutcome.SUCCESS
        );

        assertEquals(ProposalState.AWAITING_LOCATION, nextState);
    }

    @Test
    void getNextState_ShouldAdvanceFromLocationToNotes_WhenSuccess() {
        ConversationState nextState = strategy.getNextState(
                new ChatbotContext(),
                ProposalState.AWAITING_LOCATION,
                HandlerOutcome.SUCCESS
        );

        assertEquals(ProposalState.AWAITING_NOTES, nextState);
    }

    @Test
    void getNextState_ShouldAdvanceFromSubmittedToMainMenu_WhenSuccess() {
        ConversationState nextState = strategy.getNextState(
                new ChatbotContext(),
                ProposalState.SUBMITTED,
                HandlerOutcome.SUCCESS
        );

        assertEquals(CoreState.MAIN_MENU, nextState);
    }

    @Test
    void getNextState_ShouldStayInSameState_WhenFailure() {
        ConversationState nextState = strategy.getNextState(
                new ChatbotContext(),
                ProposalState.AWAITING_LOCATION,
                HandlerOutcome.FAILURE
        );

        assertEquals(ProposalState.AWAITING_LOCATION, nextState);
    }
}
