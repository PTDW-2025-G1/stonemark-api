package pt.estga.review.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a fallback MeterRegistry when no other MeterRegistry is configured in the application.
 */
@Configuration
@ConditionalOnProperty(prefix = "review.metrics", name = "fallback", havingValue = "true")
public class ReviewMetricsConfiguration {

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
