package pt.estga.decision.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.decision")
@EnableJpaRepositories("pt.estga.decision.repositories")
@EntityScan("pt.estga.decision.entities")
public class DecisionAutoConfiguration {
}
