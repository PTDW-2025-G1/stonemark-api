package pt.estga;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Enforces the module boundaries: any cross-module access that goes outside an
 * exposed named interface fails the build.
 */
class ModulithVerificationTest {

    @Test
    void verifyModules() {
        ApplicationModules.of(StonemarkApplication.class).verify();
    }
}
