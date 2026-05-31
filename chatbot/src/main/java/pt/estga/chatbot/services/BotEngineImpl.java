package pt.estga.chatbot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotEngineImpl implements BotEngine {

    private static final long CONTEXT_IDLE_TIMEOUT_MS = TimeUnit.HOURS.toMillis(2);

    private final ConversationDispatcher conversationDispatcher;
    private final CacheManager cacheManager;
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

        String conversationKey = deriveConversationKey(input, userId);
        ChatbotContext context = getOrCreateContext(conversationKey);

        // Evict stale contexts (abandoned conversations) to free memory
        if (isStale(context)) {
            log.warn("Evicting stale context for key: {} (idle since {})", conversationKey, context.getLastActivityTimestamp());
            context = new ChatbotContext();
            Objects.requireNonNull(cacheManager.getCache("conversations")).put(conversationKey, context);
        }

        ChatbotContext ctx = context;
        ctx.touch();

        currentUserId.ifPresent(id -> {
            if (ctx.getDomainUserId() == null) {
                ctx.setDomainUserId(id);
            }
        });

        authenticateUserIfPossible(ctx, input);

        if (isGlobalCommand(input)) {
            resetContext(ctx);
        }

        if (ctx.getCurrentState() == null) {
            ctx.setCurrentState(CoreState.START);
        }


        return conversationDispatcher.dispatch(ctx, input);
    }

    private ChatbotContext getOrCreateContext(String conversationKey) {
        Cache cache = cacheManager.getCache("conversations");
        if (cache == null) {
            throw new IllegalStateException("Conversation cache 'conversations' is not configured");
        }
        return cache.get(conversationKey, ChatbotContext::new);
    }

    private boolean isStale(ChatbotContext context) {
        long idle = System.currentTimeMillis() - context.getLastActivityTimestamp();
        return idle > CONTEXT_IDLE_TIMEOUT_MS;
    }

    /**
     * Derives a stable, non-null cache key for conversation contexts.
     * Priority:
     * 1. Resolved domain user id (USER:{id})
     * 2. Platform + chat id (PLATFORM:{chatId})
     * If neither can be derived an IllegalArgumentException is thrown.
     */
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
        context.getSubmissionContext().clear();
    }
}
