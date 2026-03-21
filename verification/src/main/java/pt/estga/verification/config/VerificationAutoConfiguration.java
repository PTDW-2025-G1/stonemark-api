package pt.estga.verification.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("pt.estga.verification")
@EnableJpaRepositories("pt.estga.verification.repositories")
@EntityScan("pt.estga.verification.entities")
public class VerificationAutoConfiguration {
}
