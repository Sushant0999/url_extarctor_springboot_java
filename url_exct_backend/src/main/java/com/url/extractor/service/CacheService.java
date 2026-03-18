package com.url.extractor.service;

import com.url.extractor.dto.ExtractedData;
import com.url.extractor.utils.MyLogger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheService {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long TTL_MS = 5 * 60 * 1000; // 5 minutes

    public CacheEntry get(String url) {
        CacheEntry entry = cache.get(url);
        if (entry != null && !entry.isExpired()) {
            MyLogger.info("Cache: Hit found for URL: " + url);
            return entry;
        }
        return null;
    }

    public void put(String url, ExtractedData data, String storagePath) {
        if (data != null && data.isSuccess()) {
            cache.put(url, new CacheEntry(data, storagePath, System.currentTimeMillis() + TTL_MS));
            MyLogger.info("Cache: Data saved for URL: " + url);
        }
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void cleanup() {
        int removed = 0;
        for (String url : cache.keySet()) {
            if (cache.get(url).isExpired()) {
                cache.remove(url);
                removed++;
            }
        }
        if (removed > 0) {
            MyLogger.info("Cache Cleanup: Removed " + removed + " expired entries.");
        }
    }

    public static class CacheEntry {
        private final ExtractedData data;
        private final String storagePath;
        private final long expiryTime;

        public CacheEntry(ExtractedData data, String storagePath, long expiryTime) {
            this.data = data;
            this.storagePath = storagePath;
            this.expiryTime = expiryTime;
        }

        public ExtractedData getData() {
            return data;
        }

        public String getStoragePath() {
            return storagePath;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
