package pt.estga.file.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnClass(MeterRegistry.class)
public class MediaMetricsService {

    private final Counter uploadTotal;
    private final Counter uploadSuccess;
    private final Counter uploadFailed;
    private final Counter uploadRejected;
    private final Counter variantGenerated;
    private final Counter variantFailed;
    private final DistributionSummary uploadSizeBytes;
    private final Timer uploadDuration;
    private final Timer processingDuration;

    public MediaMetricsService(MeterRegistry meterRegistry) {
        this.uploadTotal = Counter.builder("media.upload.total")
                .description("Total number of upload attempts")
                .register(meterRegistry);
        this.uploadSuccess = Counter.builder("media.upload.success")
                .description("Number of successful uploads")
                .register(meterRegistry);
        this.uploadFailed = Counter.builder("media.upload.failed")
                .description("Number of failed uploads")
                .register(meterRegistry);
        this.uploadRejected = Counter.builder("media.upload.rejected")
                .description("Number of uploads rejected by validation")
                .register(meterRegistry);
        this.variantGenerated = Counter.builder("media.variant.generated")
                .description("Number of variants generated")
                .register(meterRegistry);
        this.variantFailed = Counter.builder("media.variant.failed")
                .description("Number of variant generation failures")
                .register(meterRegistry);
        this.uploadSizeBytes = DistributionSummary.builder("media.upload.size.bytes")
                .description("Distribution of upload sizes in bytes")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        this.uploadDuration = Timer.builder("media.upload.duration")
                .description("Duration of upload operations")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        this.processingDuration = Timer.builder("media.processing.duration")
                .description("Duration of async variant processing")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void recordUploadAttempt() {
        uploadTotal.increment();
    }

    public void recordUploadSuccess(long sizeBytes, long durationMillis) {
        uploadSuccess.increment();
        uploadSizeBytes.record(sizeBytes);
        uploadDuration.record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public void recordUploadFailed() {
        uploadFailed.increment();
    }

    public void recordUploadRejected() {
        uploadRejected.increment();
    }

    public void recordVariantGenerated() {
        variantGenerated.increment();
    }

    public void recordVariantFailed() {
        variantFailed.increment();
    }

    public Timer.Sample startProcessingTimer() {
        return Timer.start();
    }

    public void recordProcessingDuration(Timer.Sample sample) {
        sample.stop(processingDuration);
    }
}
