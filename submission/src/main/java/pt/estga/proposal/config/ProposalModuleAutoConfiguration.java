package pt.estga.proposal.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("pt.estga.proposal")
@EnableJpaRepositories("pt.estga.proposal.repositories")
@EntityScan("pt.estga.proposal.entities")
@EnableCaching
public class ProposalModuleAutoConfiguration {
}
