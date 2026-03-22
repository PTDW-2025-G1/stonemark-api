package pt.estga.support.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.support")
@EnableJpaRepositories("pt.estga.support.repositories")
@EntityScan("pt.estga.contact.entities")
public class ContactAutoConfiguration {
}
