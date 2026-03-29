package pt.estga.intake.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.intake")
@EnableJpaRepositories("pt.estga.intake.repositories")
@EntityScan("pt.estga.intake.entities")
@EnableCaching
public class SubmissionAutoConfiguration {
}
