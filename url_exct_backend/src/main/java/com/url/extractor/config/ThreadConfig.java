package com.url.extractor.config;

import com.url.extractor.utils.MyLogger;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Factory
public class ThreadConfig {

    @Singleton
    @Named("urlTaskExecutor")
    public ExecutorService urlTaskExecutor() {
        return new ThreadPoolExecutor(
                5,
                10,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(25),
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "UrlTask-" + count.getAndIncrement());
                    }
                },
                (r, executor) -> MyLogger.warn("Threads are busy! Task rejected, should be sent to RabbitMQ.")
        );
    }
}
