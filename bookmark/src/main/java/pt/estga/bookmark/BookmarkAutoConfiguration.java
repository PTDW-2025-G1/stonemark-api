package pt.estga.bookmark;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.bookmark")
@EnableJpaRepositories("pt.estga.bookmark")
@EntityScan("pt.estga.bookmark")
public class BookmarkAutoConfiguration {
}
