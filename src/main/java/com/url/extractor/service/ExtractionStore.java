package com.url.extractor.service;

import com.url.extractor.dto.ExtractedData;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class ExtractionStore {
    private final AtomicReference<ExtractedData> lastResult = new AtomicReference<>();
    private final AtomicReference<String> lastStoragePath = new AtomicReference<>();

    public void save(ExtractedData data, String storagePath) {
        this.lastResult.set(data);
        this.lastStoragePath.set(storagePath);
    }

    public ExtractedData getLastResult() {
        return this.lastResult.get();
    }

    public String getLastStoragePath() {
        return this.lastStoragePath.get();
    }
}
