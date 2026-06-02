package pt.estga.boot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.user-permissions.ttl-minutes:15}")
    private long userPermissionsTtlMinutes;

    @Value("${cache.user-permissions.max-size:1000}")
    private long userPermissionsMaxSize;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("user-permissions");
        cacheManager.registerCustomCache("conversations",
                Caffeine.newBuilder()
                        .expireAfterAccess(30, TimeUnit.MINUTES)
                        .maximumSize(10_000)
                        .build());
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(userPermissionsTtlMinutes, TimeUnit.MINUTES)
                .maximumSize(userPermissionsMaxSize)
                .recordStats());
        return cacheManager;
    }
}
