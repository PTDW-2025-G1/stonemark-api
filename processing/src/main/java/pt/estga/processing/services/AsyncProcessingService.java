package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncProcessingService {

    private final ProcessingService processingService;
    private final MeterRegistry meterRegistry;

    /**
     * Offload processing to a background thread so the transaction listener can return quickly.
     * Exceptions are caught and logged to avoid killing the executor thread.
     */
    @Async("processingTaskExecutor")
    public void processAsync(Long submissionId) {
        log.info("Starting async processing for submission {}", submissionId);
        try {
            meterRegistry.counter("processing.async.invocations").increment();
            processingService.processSubmission(submissionId);
        } catch (Exception e) {
            log.error("Async processing failed for submission {}: {}", submissionId, e.getMessage(), e);
        }
    }
}
