package pt.estga.monument.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.monument")
@EnableJpaRepositories("pt.estga.monument")
@EntityScan("pt.estga.monument")
public class MonumentAutoConfiguration {
}
