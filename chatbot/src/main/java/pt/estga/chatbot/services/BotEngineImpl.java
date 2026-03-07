package pt.estga.chatbot.services;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.constants.SharedCallbackData;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.shared.models.AppPrincipal;
import pt.estga.shared.utils.SecurityUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BotEngineImpl implements BotEngine {

    private final ConversationDispatcher conversationDispatcher;
    private final Cache<String, ChatbotContext> conversationContexts;
    private final AuthServiceFactory authServiceFactory;

    @Override
    public List<BotResponse> handleInput(BotInput input) {
        String userId = input.getUserId();
        var currentUserId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            userId = currentUserId
                    .map(String::valueOf)
                    .orElse(null);
        }

        ChatbotContext context = getOrCreateContext(userId);

        currentUserId.ifPresent(id -> {
            if (context.getDomainUserId() == null) {
                context.setDomainUserId(id);
            }
        });

        authenticateUserIfPossible(context, input);

        if (isGlobalCommand(input)) {
            resetContext(context);
        }

        if (context.getCurrentState() == null) {
            context.setCurrentState(CoreState.START);
        }


        return conversationDispatcher.dispatch(context, input);
    }

    private ChatbotContext getOrCreateContext(String userId) {
        return conversationContexts.get(userId, k -> new ChatbotContext());
    }

    private void authenticateUserIfPossible(ChatbotContext context, BotInput input) {
        AuthService authService = authServiceFactory.getAuthService(input.getPlatform());
        Optional<AppPrincipal> principalOpt = authService.authenticate(input.getUserId());
        
        principalOpt.ifPresent(principal -> {
            if (context.getDomainUserId() == null) {
                context.setDomainUserId(principal.getId());
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });
    }

    private boolean isGlobalCommand(BotInput input) {
        String text = input.getText();
        String callbackData = input.getCallbackData();

        boolean isStartCommand = text != null && text.startsWith("/start");
        boolean isHelpCommand = text != null && text.startsWith("/help");
        boolean isOptionsCommand = text != null && text.startsWith("/options");
        boolean isBackToMenu = callbackData != null && callbackData.equals(SharedCallbackData.BACK_TO_MAIN_MENU);

        return isStartCommand || isHelpCommand || isOptionsCommand || isBackToMenu;
    }

    private void resetContext(ChatbotContext context) {
        context.setCurrentState(CoreState.START);
        context.getProposalContext().clear();
    }
}
