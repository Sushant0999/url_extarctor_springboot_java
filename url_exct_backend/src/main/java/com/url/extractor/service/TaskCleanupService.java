package com.url.extractor.service;

import com.url.extractor.utils.MyLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskCleanupService {

    @Autowired
    private TaskTrackerService taskTrackerService;

    @Autowired
    private ExtractionStore extractionStore;

    @Autowired
    private StorageService storageService;

    // taskId -> task completion/creation time mapping
    private final Map<String, Long> taskCompletionTimes = new ConcurrentHashMap<>();
    
    // 30 minutes TTL for extracted data and directories
    private static final long TASK_TTL_MS = 30 * 60 * 1000;

    /**
     * Mark a task's start/completion time to begin the clock for cleanup.
     * Call this when a task is completed or failed so it eventually gets erased.
     */
    public void recordTaskCompletion(String taskId) {
        taskCompletionTimes.put(taskId, System.currentTimeMillis());
    }

    /**
     * Runs every 5 minutes to sweep old tasks from memory and disk.
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupOldTasks() {
        long now = System.currentTimeMillis();
        int removedCount = 0;

        for (String taskId : Set.copyOf(taskCompletionTimes.keySet())) {
            long completionTime = taskCompletionTimes.getOrDefault(taskId, now);
            
            // If the task has exceeded its Time-To-Live
            if (now - completionTime > TASK_TTL_MS) {
                // Delete the physical files if present
                String storagePath = extractionStore.getStoragePath(taskId);
                if (storagePath != null) {
                    storageService.deleteTaskStorage(storagePath);
                }

                // Remove from in-memory stores
                extractionStore.remove(taskId);
                taskTrackerService.removeTask(taskId);
                
                // Unmark the tracking time
                taskCompletionTimes.remove(taskId);

                removedCount++;
            }
        }

        if (removedCount > 0) {
            MyLogger.info("Task Cleanup: Removed " + removedCount + " expired tasks and their directories from memory and disk.");
        }
    }
}
