package pt.estga.file.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@AutoConfiguration
@ComponentScan("pt.estga.file")
@EnableJpaRepositories("pt.estga.file.repositories")
@EntityScan("pt.estga.file.entities")
@EnableAsync
@EnableCaching
public class FileAutoConfiguration {
}
