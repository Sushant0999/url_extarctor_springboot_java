package com.url.extractor.service.impl;

import com.url.extractor.dto.ExtractedData;
import com.url.extractor.model.TaskStatus;
import com.url.extractor.service.*;
import com.url.extractor.utils.MyLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
public class ExtractionServiceImpl implements ExtractionService {

    @Autowired
    private StrategySelector strategySelector;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private JsoupStrategy jsoupStrategy;

    @Autowired
    private StorageService storageService;

    @Autowired
    private ExtractionStore extractionStore;

    @Autowired
    private UrlProducer urlProducer;

    @Autowired
    private TaskTrackerService taskTrackerService;

    @Autowired
    @Qualifier("urlTaskExecutor")
    private Executor urlTaskExecutor;

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
                MyLogger.warn("ExtractionService: Local threads busy! Offloading to RabbitMQ: " + url);
                urlProducer.sendUrl(taskId, url);
            }
        }

        return urlToTaskId;
    }

    @Autowired
    private CacheService cacheService;

    @Autowired
    private TaskCleanupService taskCleanupService;

    @Override
    public void processSingleUrl(String taskId, String url) {
        try {
            MyLogger.info("ExtractionService: Attempting to process -> " + url + " (TaskID: " + taskId + ")");
            taskTrackerService.updateStatus(taskId, TaskStatus.IN_PROGRESS);

            // 🧠 Cache Check: Skip extraction if data exists and hasn't expired (5 min TTL)
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
                
                // 🧠 Cache Store
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
