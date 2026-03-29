package pt.estga.processing.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.processing")
@EnableJpaRepositories("pt.estga.processing.repositories")
@EntityScan("pt.estga.processing.entities")
public class ProcessingAutoConfiguration {
}
