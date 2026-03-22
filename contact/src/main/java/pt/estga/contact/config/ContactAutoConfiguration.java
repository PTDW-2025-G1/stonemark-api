package pt.estga.contact.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.contact")
@EnableJpaRepositories("pt.estga.contact.repositories")
@EntityScan("pt.estga.contact.entities")
public class ContactAutoConfiguration {
}
