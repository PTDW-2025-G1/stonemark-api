package pt.estga.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import pt.estga.processing.config.ProcessingProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@EnableCaching
@SpringBootApplication
@ComponentScan(basePackages = "pt.estga")
@EnableJpaRepositories(basePackages = "pt.estga")
@EntityScan(basePackages = "pt.estga")
@EnableConfigurationProperties(ProcessingProperties.class)
public class StonemarkApplication {

	public static void main(String[] args) {
		SpringApplication.run(StonemarkApplication.class, args);
	}

}
