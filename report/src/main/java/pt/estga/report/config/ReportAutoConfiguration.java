package pt.estga.report.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.report")
@EnableJpaRepositories("pt.estga.report.repositories")
@EntityScan("pt.estga.report.entities")
public class ReportAutoConfiguration {
}
