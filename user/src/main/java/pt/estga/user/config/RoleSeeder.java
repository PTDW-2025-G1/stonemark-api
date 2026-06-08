package pt.estga.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.user.entities.Role;
import pt.estga.user.repositories.RoleRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleSeeder {

    private final RoleRepository roleRepository;

    private static final List<String> ROLE_NAMES = List.of("USER", "REVIEWER", "MODERATOR", "ADMIN");

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (roleRepository.count() > 0) {
            log.info("Roles already seeded, skipping");
            return;
        }

        log.info("Seeding roles");

        for (String name : ROLE_NAMES) {
            Role role = roleRepository.save(Role.builder()
                    .name(name)
                    .description("Pre-defined " + name.toLowerCase() + " role")
                    .build());
            log.info("Seeded role: {}", role.getName());
        }

        log.info("Role seeding complete");
    }
}
