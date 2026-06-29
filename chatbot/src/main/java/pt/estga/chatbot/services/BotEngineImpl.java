package pt.estga.chatbot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.constants.CallbackData;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.Platform;
import pt.estga.commoncore.models.AppPrincipal;
import pt.estga.commoncore.utils.SecurityUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotEngineImpl implements BotEngine {

    private static final Set<String> SLASH_COMMANDS = Set.of("/start", "/help", "/options");

    private final ConversationDispatcher conversationDispatcher;
    private final CacheManager cacheManager;
    private final List<AuthService> authServices;

    @Override
    public List<BotResponse> handleInput(BotInput input) {
        String userId = input.getUserId();
        var currentUserId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            userId = currentUserId
                    .map(String::valueOf)
                    .orElse(null);
        }

        String conversationKey = deriveConversationKey(input, userId);
        ChatbotContext context = getOrCreateContext(conversationKey);

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

    private ChatbotContext getOrCreateContext(String conversationKey) {
        Cache cache = cacheManager.getCache("conversations");
        if (cache == null) {
            throw new IllegalStateException("Conversation cache 'conversations' is not configured");
        }
        return cache.get(conversationKey, () -> new ChatbotContext());
    }

    private String deriveConversationKey(BotInput input, String resolvedUserId) {
        if (resolvedUserId != null) {
            return "USER:" + resolvedUserId;
        }
        if (input.getPlatform() != null && input.getChatId() != 0L) {
            return input.getPlatform().name() + ":" + input.getChatId();
        }
        throw new IllegalArgumentException("Cannot derive conversation key from input: missing userId and platform/chatId");
    }

    private void authenticateUserIfPossible(ChatbotContext context, BotInput input) {
        AuthService authService = resolveAuthService(input.getPlatform());
        Optional<AppPrincipal> principalOpt = authService.authenticate(input.getUserId());
        
        principalOpt.ifPresent(principal -> {
            if (context.getDomainUserId() == null) {
                context.setDomainUserId(principal.getId());
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });
    }

    private AuthService resolveAuthService(Platform platform) {
        return authServices.stream()
                .filter(s -> s.supports(platform))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No AuthService found for platform: " + platform));
    }

    private static boolean isGlobalCommand(BotInput input) {
        return (input.getText() != null && SLASH_COMMANDS.contains(input.getText()))
                || CallbackData.BACK_TO_MAIN_MENU.equals(input.getCallbackData());
    }

    private void resetContext(ChatbotContext context) {
        context.setCurrentState(CoreState.START);
        context.getSubmissionContext().clear();
    }
}
