package pt.estga.boot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import pt.estga.shared.models.AuditActor;
import pt.estga.shared.utils.SecurityUtils;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditingConfig {

    @Bean
    AuditorAware<AuditActor> auditorAware() {
        return () -> SecurityUtils.currentPrincipal()
                .map(p -> new AuditActor(
                        p.getId(),
                        p.getIdentifier()
                ));
    }
}
