package pt.estga.processing.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the processing module's configuration properties inside the module,
 * so the composition root does not need to reference processing internals.
 */
@Configuration
@EnableConfigurationProperties(ProcessingProperties.class)
public class ProcessingConfig {
}
