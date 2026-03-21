package pt.estga.user.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("pt.estga.user")
@EnableJpaRepositories("pt.estga.user")
@EntityScan("pt.estga.user.entities")
public class UserAutoConfiguration {
}
