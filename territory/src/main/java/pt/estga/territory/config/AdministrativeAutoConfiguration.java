package pt.estga.territory.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@AutoConfiguration
@ComponentScan("pt.estga.territory")
@EnableJpaRepositories("pt.estga.territory.repositories")
@EntityScan("pt.estga.territory.entities")
@EnableAsync
public class AdministrativeAutoConfiguration {
}
