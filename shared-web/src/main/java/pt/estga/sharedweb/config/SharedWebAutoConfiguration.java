package pt.estga.sharedweb.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import pt.estga.sharedweb.filtering.QueryProcessor;
import pt.estga.sharedweb.filtering.SpecificationBuilder;

@AutoConfiguration
@ComponentScan("pt.estga.sharedweb")
public class SharedWebAutoConfiguration {

    /**
     * Provides a prototype-scoped QueryProcessor.
     * Prototype scope is used here because QueryProcessor is a lightweight wrapper
     * around a generic type T, ensuring clean separation for different entities.
     */
    @Bean
    @Scope("prototype")
    public <T> QueryProcessor<T> queryProcessor(SpecificationBuilder<T> builder) {
        return new QueryProcessor<>(builder);
    }
}
