package pt.estga.chatbot.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.estga.chatbot.context.ChatbotContext;

import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnMissingBean(CacheManager.class)
public class ChatbotCacheConfig {

    @Bean
    public Cache<String, ChatbotContext> conversationContextCache() {
        return Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }
}
