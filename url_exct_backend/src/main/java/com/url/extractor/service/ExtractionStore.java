package com.url.extractor.service;

import com.url.extractor.dto.ExtractedData;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe store that holds per-task extraction results and their storage paths.
 * Replaces the old single-entry AtomicReference approach to support multiple concurrent tasks.
 */
@Service
public class ExtractionStore {

    // taskId -> ExtractedData
    private final Map<String, ExtractedData> resultsByTaskId = new ConcurrentHashMap<>();

    // taskId -> storagePath on disk
    private final Map<String, String> storagePathByTaskId = new ConcurrentHashMap<>();

    /**
     * Save (or overwrite) the result for the given taskId.
     */
    public void save(String taskId, ExtractedData data, String storagePath) {
        resultsByTaskId.put(taskId, data);
        storagePathByTaskId.put(taskId, storagePath);
    }

    /**
     * Get the extracted result for a specific taskId.
     */
    public ExtractedData getResult(String taskId) {
        return resultsByTaskId.get(taskId);
    }

    /**
     * Get the storage path for a specific taskId.
     */
    public String getStoragePath(String taskId) {
        return storagePathByTaskId.get(taskId);
    }

    /**
     * Get all stored results (taskId -> ExtractedData).
     */
    public Map<String, ExtractedData> getAllResults() {
        return Map.copyOf(resultsByTaskId);
    }

    /**
     * Remove results for a specific taskId (cleanup).
     */
    public void remove(String taskId) {
        resultsByTaskId.remove(taskId);
        storagePathByTaskId.remove(taskId);
    }
}
