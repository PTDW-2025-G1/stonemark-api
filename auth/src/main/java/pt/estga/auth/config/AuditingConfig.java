package pt.estga.auth.config;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import pt.estga.shared.core.models.AppPrincipal;
import pt.estga.shared.core.utils.SecurityUtils;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditingConfig {

    @Bean
    AuditorAware<UUID> auditorAware() {
        return () -> SecurityUtils.currentPrincipal()
                .map(AppPrincipal::getId);
    }
}
