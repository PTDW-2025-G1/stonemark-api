package pt.estga.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.user.entities.Permission;
import pt.estga.user.entities.Role;
import pt.estga.user.repositories.PermissionRepository;
import pt.estga.user.repositories.RoleRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RolePermissionSeeder {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (roleRepository.count() > 0 && permissionRepository.count() > 0) {
            log.info("Roles and permissions already seeded, skipping");
            return;
        }

        log.info("Seeding roles and permissions");

        Permission userRead = permissionRepository.save(Permission.builder()
                .name("USER_READ").description("View user profiles").build());
        Permission userWrite = permissionRepository.save(Permission.builder()
                .name("USER_WRITE").description("Edit own profile").build());
        Permission userManage = permissionRepository.save(Permission.builder()
                .name("USER_MANAGE").description("Manage all users").build());
        Permission monumentRead = permissionRepository.save(Permission.builder()
                .name("MONUMENT_READ").description("View monuments").build());
        Permission monumentWrite = permissionRepository.save(Permission.builder()
                .name("MONUMENT_WRITE").description("Create and edit monuments").build());
        Permission marksManage = permissionRepository.save(Permission.builder()
                .name("MARKS_MANAGE").description("Manage mason marks").build());
        Permission evidenceManage = permissionRepository.save(Permission.builder()
                .name("EVIDENCE_MANAGE").description("Manage mark evidence").build());
        Permission occurrenceManage = permissionRepository.save(Permission.builder()
                .name("OCCURRENCE_MANAGE").description("Manage mark occurrences").build());
        Permission suggestionsRead = permissionRepository.save(Permission.builder()
                .name("SUGGESTIONS_READ").description("View processing suggestions").build());
        Permission submissionsManage = permissionRepository.save(Permission.builder()
                .name("SUBMISSIONS_MANAGE").description("Manage evidence submissions").build());
        Permission processingView = permissionRepository.save(Permission.builder()
                .name("PROCESSING_VIEW").description("View processing status").build());
        Permission reviewSubmit = permissionRepository.save(Permission.builder()
                .name("REVIEW_SUBMIT").description("Submit review suggestions").build());
        Permission reviewModerate = permissionRepository.save(Permission.builder()
                .name("REVIEW_MODERATE").description("Moderate and manage reviews").build());
        Permission contactManage = permissionRepository.save(Permission.builder()
                .name("CONTACT_MANAGE").description("Manage contact requests").build());
        Permission importData = permissionRepository.save(Permission.builder()
                .name("IMPORT_DATA").description("Import bulk data").build());
        Permission chatbotUse = permissionRepository.save(Permission.builder()
                .name("CHATBOT_USE").description("Use chatbot features").build());

        Map<String, Set<Permission>> rolePermissions = Map.of(
                "USER", Set.of(userRead, userWrite, monumentRead, chatbotUse),
                "REVIEWER", Set.of(userRead, userWrite, monumentRead, chatbotUse,
                        reviewSubmit, reviewModerate, suggestionsRead, processingView),
                "MODERATOR", Set.of(userRead, userWrite, monumentRead, chatbotUse,
                        reviewSubmit, reviewModerate,
                        monumentWrite, contactManage, importData,
                        marksManage, evidenceManage, occurrenceManage, suggestionsRead,
                        submissionsManage, processingView),
                "ADMIN", Set.of(userRead, userWrite, userManage,
                        monumentRead, monumentWrite,
                        marksManage, evidenceManage, occurrenceManage, suggestionsRead,
                        submissionsManage, processingView,
                        reviewSubmit, reviewModerate,
                        contactManage, importData,
                        chatbotUse)
        );

        for (var entry : rolePermissions.entrySet()) {
            Role role = roleRepository.save(Role.builder()
                    .name(entry.getKey())
                    .description("Pre-defined " + entry.getKey().toLowerCase() + " role")
                    .permissions(entry.getValue())
                    .build());
            log.info("Seeded role: {} with {} permissions", role.getName(), entry.getValue().size());
        }

        log.info("Role and permission seeding complete");
    }
}
