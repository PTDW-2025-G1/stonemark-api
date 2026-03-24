package pt.estga.shared.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes application events after the surrounding transaction successfully commits.
 * This prevents event handlers from acting on changes that may still be rolled back.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AfterCommitEventPublisher {

    private final ApplicationEventPublisher delegate;

    public void publish(Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        delegate.publishEvent(event);
                    } catch (Exception e) {
                        log.error("Failed to publish event after commit: {}", event, e);
                    }
                }
            });
        } else {
            delegate.publishEvent(event);
        }
    }
}
