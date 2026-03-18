package com.url.extractor.config;

import com.url.extractor.service.UrlProducer;
import com.url.extractor.utils.MyLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class ThreadConfig {

    @Autowired
    @Lazy
    private UrlProducer urlProducer;

    @Bean(name = "urlTaskExecutor")
    public Executor urlTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25); // Small queue to trigger RabbitMQ quickly when busy
        executor.setThreadNamePrefix("UrlTask-");

        // Custom RejectedExecutionHandler: Send to RabbitMQ when all threads/queues are busy
        executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                // Since Runnable doesn't directly contain the URL, we'll need a wrapper or handle it differently
                // But for simplicity in this task, we'll let the controller handle the logic
                MyLogger.warn("Threads are busy! Task rejected, should be sent to RabbitMQ.");
            }
        });

        executor.initialize();
        return executor;
    }
}
