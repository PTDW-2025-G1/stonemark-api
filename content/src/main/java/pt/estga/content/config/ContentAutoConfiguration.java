package pt.estga.content.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.content")
@EnableJpaRepositories("pt.estga.content.repositories")
@EntityScan("pt.estga.content.entities")
public class ContentAutoConfiguration {
}
