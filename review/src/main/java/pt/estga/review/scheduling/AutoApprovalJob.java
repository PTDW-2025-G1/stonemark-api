package pt.estga.review.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.estga.review.services.AutoApprovalService;

/**
 * Periodically approves confident, unambiguous matches without a human. Created
 * only when {@code review.auto-approve.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "review.auto-approve.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class AutoApprovalJob {

    private final AutoApprovalService autoApprovalService;

    @Scheduled(fixedDelayString = "${review.auto-approve.interval-ms:300000}")
    public void run() {
        try {
            int approved = autoApprovalService.approveEligible();
            if (approved > 0) {
                log.info("Auto-approved {} submission(s)", approved);
            }
        } catch (Exception e) {
            log.warn("Auto-approval run failed: {}", e.getMessage());
        }
    }
}
