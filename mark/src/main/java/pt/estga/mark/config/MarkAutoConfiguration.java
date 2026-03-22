package pt.estga.mark.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.mark")
@EnableJpaRepositories("pt.estga.mark.repositories")
@EntityScan("pt.estga.mark.entities")
public class MarkAutoConfiguration {
}
