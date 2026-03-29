package pt.estga.bookmark.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("pt.estga.bookmark")
@EnableJpaRepositories("pt.estga.bookmark.repositories")
@EntityScan("pt.estga.bookmark.emtities")
public class BookmarkAutoConfiguration {
}
