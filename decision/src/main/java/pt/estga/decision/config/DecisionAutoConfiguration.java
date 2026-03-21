package pt.estga.decision.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("pt.estga.decision")
@EnableJpaRepositories("pt.estga.decision.repositories")
@EntityScan("pt.estga.decision.entities")
public class DecisionAutoConfiguration {
}
