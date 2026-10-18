package pt.estga;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import pt.estga.mark.repositories.MarkRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full application context and asserts the core pieces are wired: the
 * entry point, the security filter chain, JPA repositories and the OpenAPI bean.
 *
 * <p>Runs against in-memory H2 with no schema (see {@code application-test.yml}), so
 * it proves wiring — beans, configuration, security, persistence bootstrap — rather
 * than database behaviour. A data-level smoke test needs a real PostGIS/pgvector
 * database.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationBootSmokeTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private OpenAPI openAPI;

    @Test
    @DisplayName("boots with the security chain, JPA repositories and OpenAPI wired")
    void bootsWithCoreBeans() {
        assertThat(context.getBean(StonemarkApplication.class)).isNotNull();
        assertThat(context.getBean(SecurityFilterChain.class)).isNotNull();
        assertThat(context.getBean(MarkRepository.class)).isNotNull();
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isNotBlank();
    }
}
