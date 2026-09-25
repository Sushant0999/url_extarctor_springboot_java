package com.url.extractor.service.impl;

import com.url.extractor.dto.ExtractedData;
import com.url.extractor.model.TaskStatus;
import com.url.extractor.service.*;
import com.url.extractor.utils.MyLogger;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@Singleton
public class ExtractionServiceImpl implements ExtractionService {

    @Inject
    private StrategySelector strategySelector;

    @Inject
    private AnalysisService analysisService;

    @Inject
    private JsoupStrategy jsoupStrategy;

    @Inject
    private StorageService storageService;

    @Inject
    private ExtractionStore extractionStore;

    @Inject
    @Nullable
    private UrlProducer urlProducer;

    @Inject
    private TaskTrackerService taskTrackerService;

    @Inject
    @Named("urlTaskExecutor")
    private ExecutorService urlTaskExecutor;

    @Inject
    private CacheService cacheService;

    @Inject
    private TaskCleanupService taskCleanupService;

    @Override
    public Map<String, String> processBulk(List<String> urls) {
        Map<String, String> urlToTaskId = new HashMap<>();
        if (urls == null || urls.isEmpty()) {
            return urlToTaskId;
        }

        for (String url : urls) {
            String taskId = UUID.randomUUID().toString();
            urlToTaskId.put(url, taskId);
            taskTrackerService.createTask(taskId);

            try {
                urlTaskExecutor.execute(() -> processSingleUrl(taskId, url));
            } catch (RejectedExecutionException e) {
                if (urlProducer != null) {
                    MyLogger.warn("ExtractionService: Local threads busy! Offloading to RabbitMQ: " + url);
                    urlProducer.sendUrl(taskId, url);
                } else {
                    MyLogger.err("ExtractionService: Local threads busy and RabbitMQ is disabled! Dropping URL: " + url);
                    taskTrackerService.failTask(taskId);
                }
            }
        }

        return urlToTaskId;
    }

    @Override
    public void processSingleUrl(String taskId, String url) {
        try {
            MyLogger.info("ExtractionService: Attempting to process -> " + url + " (TaskID: " + taskId + ")");
            taskTrackerService.updateStatus(taskId, TaskStatus.IN_PROGRESS);

            // Cache Check: Skip extraction if data exists and hasn't expired (5 min TTL)
            CacheService.CacheEntry cachedEntry = cacheService.get(url);
            if (cachedEntry != null) {
                MyLogger.info("ExtractionService: Serving from cache for " + url);
                extractionStore.save(taskId, cachedEntry.getData(), cachedEntry.getStoragePath());
                taskTrackerService.completeTask(taskId, cachedEntry.getData());
                return;
            }

            ExtractedData extractedData = strategySelector.extract(url);
            if (extractedData != null && extractedData.isSuccess()) {
                // Enhanced Analysis (SEO, Tech, Colors, Summary)
                try {
                    org.jsoup.nodes.Document doc = jsoupStrategy.getDocument(url);
                    extractedData.setSeoIssues(analysisService.performSeoAudit(doc, extractedData));
                    extractedData.setTechStack(analysisService.detectTechStack(doc));
                    extractedData.setColorPalette(analysisService.extractColorPalette(doc));
                    extractedData.setSummary(analysisService.generateSummary(extractedData));
                } catch (Exception e) {
                    MyLogger.warn("ExtractionService: Enhanced analysis failed for " + url + ": " + e.getMessage());
                }

                String storagePath = storageService.saveExtractedData(extractedData);
                extractionStore.save(taskId, extractedData, storagePath);
                
                // Cache Store
                cacheService.put(url, extractedData, storagePath);
                
                taskTrackerService.completeTask(taskId, extractedData);
                MyLogger.info("ExtractionService: Completed " + url);
            } else {
                taskTrackerService.failTask(taskId);
                MyLogger.err("ExtractionService: Core extraction failed for " + url);
            }
        } catch (Exception e) {
            taskTrackerService.failTask(taskId);
            MyLogger.err("ExtractionService: Error processing URL " + url + ": " + e.getMessage());
        } finally {
            taskCleanupService.recordTaskCompletion(taskId);
        }
    }
}
