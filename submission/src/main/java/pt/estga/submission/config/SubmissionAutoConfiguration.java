package pt.estga.submission.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.submission")
@EnableJpaRepositories("pt.estga.submission.repositories")
@EntityScan("pt.estga.submission.entities")
@EnableCaching
public class SubmissionAutoConfiguration {
}
