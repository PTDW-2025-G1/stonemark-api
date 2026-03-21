package pt.estga.filterutils.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import pt.estga.filterutils.QueryProcessor;
import pt.estga.filterutils.SpecificationBuilder;

@Configuration
public class FilterConfig {

    @Bean
    @Scope("prototype")
    public <T> QueryProcessor<T> queryProcessor(SpecificationBuilder<T> builder) {
        return new QueryProcessor<>(builder);
    }
}
