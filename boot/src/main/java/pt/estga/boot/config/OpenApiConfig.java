package pt.estga.boot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${application.base-url}")
    private String baseUrl;

    @Bean
    public GroupedOpenApi account() {
        return GroupedOpenApi.builder()
                .group("Account")
                .pathsToMatch("/api/v1/account/**")
                .build();
    }

    @Bean
    public GroupedOpenApi publicRoutes() {
        return GroupedOpenApi.builder()
                .group("Public routes")
                .pathsToMatch("/api/v1/public/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminRoutes() {
        return GroupedOpenApi.builder()
                .group("Admin routes")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }

    @Bean
    public OpenAPI securedOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url(baseUrl))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("StoneMark API")
                        .version("v1.0")
                        .description("REST API for StoneMark application."));
    }
}
