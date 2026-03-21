package pt.estga.report.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("pt.estga.report")
@EnableJpaRepositories("pt.estga.report.repositories")
@EntityScan("pt.estga.report.entities")
public class ReportAutoConfiguration {
}
