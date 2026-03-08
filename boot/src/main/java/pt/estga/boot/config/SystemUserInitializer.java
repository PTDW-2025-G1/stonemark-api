package pt.estga.boot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import pt.estga.shared.enums.UserRole;
import pt.estga.user.entities.ServiceAccount;
import pt.estga.user.entities.User;
import pt.estga.user.enums.ServiceRole;
import pt.estga.user.services.ServiceAccountService;
import pt.estga.user.services.UserService;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SystemUserInitializer {

    private final UserService userService;
    private final ServiceAccountService serviceAccountService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    @Bean
    public CommandLineRunner initSystemUser() {
        return args -> transactionTemplate.execute(status -> {
            initServiceAccounts();
            initSystemAdminUser();
            return null;
        });
    }

    private void initServiceAccounts() {
        createServiceAccountIfNotExists(0L, "System", ServiceRole.SYSTEM);
        createServiceAccountIfNotExists(1L, "WebClient", ServiceRole.WEBCLIENT);
        createServiceAccountIfNotExists(2L, "TelegramBot", ServiceRole.CHATBOT);
        createServiceAccountIfNotExists(3L, "WhatsAppBot", ServiceRole.CHATBOT);
    }

    private void createServiceAccountIfNotExists(Long id, String name, ServiceRole role) {
        if (!serviceAccountService.existsById(id)) {
            log.info("Initializing Service Account: {} with ID {}", name, id);

            ServiceAccount serviceAccount = ServiceAccount.builder()
                    .id(id)
                    .name(name)
                    .enabled(true)
                    .secretHash(passwordEncoder.encode(name.toLowerCase() + "_secret"))
                    .role(role)
                    .build();

            serviceAccountService.create(serviceAccount);
            
            log.info("Service Account {} initialized successfully.", name);
        }
    }

    private void initSystemAdminUser() {
        if (!userService.existsByUsername("system_admin")) {
            log.info("Initializing System Admin user");
            
            User systemAdmin = User.builder()
                    .firstName("System")
                    .lastName("Administrator")
                    .username("system_admin")
                    .role(UserRole.ADMIN)
                    .enabled(true)
                    .accountLocked(false)
                    .build();

            userService.create(systemAdmin);
            
            log.info("System Admin user initialized successfully.");
        }
    }
}
