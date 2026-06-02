package pt.estga.processing.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class ProcessingAsyncConfig {

    @Bean(name = "processingTaskExecutor")
    public Executor processingTaskExecutor(MeterRegistry meterRegistry) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("processing-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        meterRegistry.gauge("processing.executor.queue_size", executor,
                ex -> ex.getThreadPoolExecutor().getQueue().size());
        meterRegistry.gauge("processing.executor.active_threads", executor,
                ex -> ex.getThreadPoolExecutor().getActiveCount());
        meterRegistry.gauge("processing.executor.pool_size", executor,
                ex -> ex.getThreadPoolExecutor().getPoolSize());

        return executor;
    }
}
