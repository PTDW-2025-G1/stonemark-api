package pt.estga.territory.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@ComponentScan("pt.estga.territory")
@EnableJpaRepositories("pt.estga.territory.repositories")
@EntityScan("pt.estga.territory.entities")
@EnableAsync
public class AdministrativeAutoConfiguration {
}
