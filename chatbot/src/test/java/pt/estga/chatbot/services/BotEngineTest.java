package pt.estga.chatbot.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.context.SecurityContextHolder;
import pt.estga.chatbot.constants.CallbackData;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.telegram.services.TelegramAuthService;
import pt.estga.commoncore.models.AppPrincipal;
import pt.estga.commoncore.utils.SecurityUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotEngineTest {

    @Mock
    private ConversationDispatcher conversationDispatcher;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private TelegramAuthService telegramAuthService;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private BotEngine engine;
    private ChatbotContext context;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);

        lenient().when(cacheManager.getCache("conversations")).thenReturn(cache);

        context = new ChatbotContext();
        lenient().when(cache.get(any(String.class), ArgumentMatchers.<java.util.concurrent.Callable<ChatbotContext>>any())).thenReturn(context);

        engine = new BotEngine(conversationDispatcher, cacheManager, telegramAuthService);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldDeriveKeyFromUserIdAndDispatch() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(42L));

        BotInput input = BotInput.builder()
                .platform(Platform.TELEGRAM)
                .chatId(1L)
                .type(BotInput.InputType.TEXT)
                .text("hello")
                .build();

        List<BotResponse> expected = List.of(BotResponse.builder().build());
        when(conversationDispatcher.dispatch(any(), any())).thenReturn(expected);

        List<BotResponse> result = engine.handleInput(input);

        assertThat(result).isEqualTo(expected);
        assertThat(context.getDomainUserId()).isEqualTo(42L);
        assertThat(context.getCurrentState()).isEqualTo(CoreState.START);
    }

    @Test
    void shouldDeriveKeyFromPlatformAndChatIdWhenNoUserId() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.empty());

        BotInput input = BotInput.builder()
                .userId("telegram_123")
                .platform(Platform.TELEGRAM)
                .chatId(777L)
                .type(BotInput.InputType.TEXT)
                .text("hello")
                .build();

        when(conversationDispatcher.dispatch(any(), any())).thenReturn(List.of());

        engine.handleInput(input);
    }

    @Test
    void shouldThrowWhenNoKeyDerivable() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.empty());

        BotInput input = BotInput.builder()
                .platform(Platform.TELEGRAM)
                .chatId(0L)
                .type(BotInput.InputType.TEXT)
                .text("hello")
                .build();

        assertThatThrownBy(() -> engine.handleInput(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot derive conversation key");
    }

    @Test
    void shouldThrowWhenCacheNotConfigured() {
        when(cacheManager.getCache("conversations")).thenReturn(null);

        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(1L));

        BotInput input = BotInput.builder()
                .platform(Platform.TELEGRAM)
                .chatId(1L)
                .type(BotInput.InputType.TEXT)
                .text("hello")
                .build();

        assertThatThrownBy(() -> engine.handleInput(input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void shouldAuthenticateUserAndSetSecurityContext() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(42L));

        AppPrincipal principal = AppPrincipal.builder()
                .id(42L)
                .identifier("testuser")
                .build();

        when(telegramAuthService.authenticate("telegram_123")).thenReturn(Optional.of(principal));

        BotInput input = BotInput.builder()
                .userId("telegram_123")
                .platform(Platform.TELEGRAM)
                .chatId(1L)
                .type(BotInput.InputType.TEXT)
                .text("hello")
                .build();

        when(conversationDispatcher.dispatch(any(), any())).thenReturn(List.of());

        engine.handleInput(input);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void shouldNotOverrideExistingDomainUserId() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(42L));
        context.setDomainUserId(99L);

        BotInput input = BotInput.builder()
                .platform(Platform.TELEGRAM)
                .chatId(1L)
                .type(BotInput.InputType.TEXT)
                .text("hello")
                .build();

        when(conversationDispatcher.dispatch(any(), any())).thenReturn(List.of());

        engine.handleInput(input);

        assertThat(context.getDomainUserId()).isEqualTo(99L);
    }

    @Test
    void shouldResetContextOnSlashCommand() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(1L));
        context.setCurrentState(CoreState.MAIN_MENU);
        context.getSubmissionContext().setPhotoFilename("test.jpg");

        BotInput input = BotInput.builder()
                .platform(Platform.TELEGRAM)
                .chatId(1L)
                .type(BotInput.InputType.TEXT)
                .text("/start")
                .build();

        when(conversationDispatcher.dispatch(any(), any())).thenReturn(List.of());

        engine.handleInput(input);

        assertThat(context.getCurrentState()).isEqualTo(CoreState.START);
        assertThat(context.getSubmissionContext().getPhotoFilename()).isNull();
    }

    @Test
    void shouldResetContextOnBackToMainMenuCallback() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(1L));
        context.setCurrentState(CoreState.MAIN_MENU);
        context.getSubmissionContext().setPhotoFilename("test.jpg");

        BotInput input = BotInput.builder()
                .platform(Platform.TELEGRAM)
                .chatId(1L)
                .type(BotInput.InputType.CALLBACK)
                .callbackData(CallbackData.BACK_TO_MAIN_MENU)
                .build();

        when(conversationDispatcher.dispatch(any(), any())).thenReturn(List.of());

        engine.handleInput(input);

        assertThat(context.getCurrentState()).isEqualTo(CoreState.START);
    }

    @Test
    void shouldNotResetContextOnNonGlobalCommand() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(1L));
        context.setCurrentState(CoreState.MAIN_MENU);

        BotInput input = BotInput.builder()
                .platform(Platform.TELEGRAM)
                .chatId(1L)
                .type(BotInput.InputType.TEXT)
                .text("some random message")
                .build();

        when(conversationDispatcher.dispatch(any(), any())).thenReturn(List.of());

        engine.handleInput(input);

        assertThat(context.getCurrentState()).isEqualTo(CoreState.MAIN_MENU);
    }
}
