package pt.estga.verification.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.verification")
@EnableJpaRepositories("pt.estga.verification.repositories")
@EntityScan("pt.estga.verification.entities")
public class VerificationAutoConfiguration {
}
