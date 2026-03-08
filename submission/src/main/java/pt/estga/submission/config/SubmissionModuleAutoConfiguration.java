package pt.estga.submission.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("pt.estga.submission")
@EnableJpaRepositories("pt.estga.submission.repositories")
@EntityScan("pt.estga.submission.entities")
@EnableCaching
public class SubmissionModuleAutoConfiguration {
}
