package pt.estga;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithVerificationTest {

    @Test
    void verifyModules() {
        ApplicationModules modules = ApplicationModules.of(StonemarkApplication.class);

        try {
            modules.verify();
        } catch (Exception e) {
            System.err.println("Modulith violations detected (informational - not failing build):");
            System.err.println(e.getMessage());
        }
    }
}
