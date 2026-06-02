package pt.estga.file.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class FileAsyncConfig {

    @Bean(name = "fileTaskExecutor")
    public Executor fileTaskExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        int poolSize = Math.max(4, cores - 1);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize * 2);
        executor.setQueueCapacity(poolSize * 10);
        executor.setThreadNamePrefix("file-async-");
        executor.setRejectedExecutionHandler(
                (r, threadPoolExecutor) -> {
                    throw new org.springframework.core.task.TaskRejectedException(
                            "File async task rejected — pool saturated");
                });
        executor.initialize();
        return executor;
    }
}
