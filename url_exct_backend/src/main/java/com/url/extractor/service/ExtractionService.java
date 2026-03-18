package com.url.extractor.service;

import java.util.List;
import java.util.Map;

public interface ExtractionService {
    /**
     * Top-level method to handle bulk requests with parallel/fallback logic.
     * Returns a map of URL to taskId.
     */
    Map<String, String> processBulk(List<String> urls);

    /**
     * Core method to extract and save data for a single URL.
     * Can be called either by a local thread or a RabbitMQ consumer.
     */
    void processSingleUrl(String taskId, String url);
}
