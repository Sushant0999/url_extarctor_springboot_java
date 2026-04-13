package com.url.extractor.config;

import com.url.extractor.utils.MyLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Semaphore;

@Configuration
public class PlaywrightConfig {

    @Bean(name = "playwrightSemaphore")
    public Semaphore playwrightSemaphore() {
        int concurrency = getOptimalBrowserConcurrency();
        MyLogger.info("Hardware Check: Setting Playwright global concurrency limit to: " + concurrency);
        return new Semaphore(concurrency);
    }

    private int getOptimalBrowserConcurrency() {
        try {
            // Using com.sun.management.OperatingSystemMXBean to get physical/container memory limit
            com.sun.management.OperatingSystemMXBean osBean = 
                ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
            
            long totalRamBytes = osBean.getTotalMemorySize();
            long totalRamMb = totalRamBytes / (1024 * 1024);
            
            MyLogger.info("Hardware Check: Detected Total System/Container Memory = " + totalRamMb + " MB");
            
            // Base OS and Java JVM baseline takes around ~300MB. 
            // Every Playwright Chromium browser consumes roughly ~200MB.
            int maxBrowsersByMemory = (int) ((totalRamMb - 300) / 200);
            
            // Limit by CPU cores so we don't overkill the CPU either
            int cores = Runtime.getRuntime().availableProcessors();
            
            int finalConcurrency = Math.min(maxBrowsersByMemory, cores * 2);
            
            // Always ensure at least 1 browser is allowed to run regardless of tight constraints
            if (finalConcurrency < 1) {
                return 1;
            }
            return finalConcurrency;
            
        } catch (Throwable e) {
            MyLogger.warn("Hardware Check: Could not accurately determine memory, defaulting to strict 1 browser limit. Reason: " + e.getMessage());
            return 1;
        }
    }
}
