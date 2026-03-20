package com.url.extractor.service;

import com.url.extractor.utils.MyLogger;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.File;
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
    
    // 5 minutes TTL for extracted data and directories
    private static final long TASK_TTL_MS = 5 * 60 * 1000;
    private static final String BASE_STORAGE_PATH = "extracted_data";

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

        // 1. Cleanup tasks tracked in memory
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

        // 2. Directory Sweep: Cleanup any untracked folders in 'extracted_data' older than TTL
        // This handles cases where the app restarted and lost in-memory tracking.
        cleanupUntrackedFolders(now);

        if (removedCount > 0) {
            MyLogger.info("Task Cleanup: Removed " + removedCount + " expired tasks from memory.");
        }
    }

    private void cleanupUntrackedFolders(long now) {
        File baseDir = new File(BASE_STORAGE_PATH);
        if (!baseDir.exists() || !baseDir.isDirectory()) return;

        File[] folders = baseDir.listFiles(File::isDirectory);
        if (folders == null) return;

        int sweptCount = 0;
        for (File folder : folders) {
            // If folder is older than TTL
            if (now - folder.lastModified() > TASK_TTL_MS) {
                storageService.deleteTaskStorage(folder.getAbsolutePath());
                sweptCount++;
            }
        }

        if (sweptCount > 0) {
            MyLogger.info("Directory Sweep: Removed " + sweptCount + " untracked old folders from " + BASE_STORAGE_PATH);
        }
    }
}
