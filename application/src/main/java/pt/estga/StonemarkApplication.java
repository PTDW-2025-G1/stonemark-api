package pt.estga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@EnableCaching
@SpringBootApplication
public class StonemarkApplication {

	public static void main(String[] args) {
		SpringApplication.run(StonemarkApplication.class, args);
	}

}
