package pt.estga.content.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("pt.estga.content")
@EnableJpaRepositories("pt.estga.content.repositories")
@EntityScan("pt.estga.content.entities")
public class ContentAutoConfiguration {
}
