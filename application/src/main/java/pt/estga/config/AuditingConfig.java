package pt.estga.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import pt.estga.commoncore.utils.SecurityUtils;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditingConfig {

    @Bean
    AuditorAware<Long> auditorAware() {
        return () -> SecurityUtils.currentPrincipal()
                .map(p -> p.getId());
    }
}
