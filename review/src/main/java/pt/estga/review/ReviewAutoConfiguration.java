package pt.estga.review;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.review")
@EnableJpaRepositories("pt.estga.review")
@EntityScan("pt.estga.review")
public class ReviewAutoConfiguration {
}
