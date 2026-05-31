package pt.estga.file.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
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

    public MediaMetricsService(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            this.uploadTotal = null;
            this.uploadSuccess = null;
            this.uploadFailed = null;
            this.uploadRejected = null;
            this.variantGenerated = null;
            this.variantFailed = null;
            this.uploadSizeBytes = null;
            this.uploadDuration = null;
            this.processingDuration = null;
            return;
        }
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
        if (uploadTotal != null) uploadTotal.increment();
    }

    public void recordUploadSuccess(long sizeBytes, long durationMillis) {
        if (uploadSuccess != null) {
            uploadSuccess.increment();
            uploadSizeBytes.record(sizeBytes);
            uploadDuration.record(durationMillis, TimeUnit.MILLISECONDS);
        }
    }

    public void recordUploadFailed() {
        if (uploadFailed != null) uploadFailed.increment();
    }

    public void recordUploadRejected() {
        if (uploadRejected != null) uploadRejected.increment();
    }

    public void recordVariantGenerated() {
        if (variantGenerated != null) variantGenerated.increment();
    }

    public void recordVariantFailed() {
        if (variantFailed != null) variantFailed.increment();
    }

    public Timer.Sample startProcessingTimer() {
        return processingDuration != null ? Timer.start() : null;
    }

    public void recordProcessingDuration(Timer.Sample sample) {
        if (sample != null && processingDuration != null) sample.stop(processingDuration);
    }
}
