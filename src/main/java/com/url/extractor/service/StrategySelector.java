package com.url.extractor.service;

import com.url.extractor.dto.ExtractedData;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class StrategySelector {

    private final JsoupStrategy jsoupStrategy;
    private final ApiStrategy apiStrategy;
    private final PlaywrightStrategy playwrightStrategy;
    private final DetectionService detectionService;

    public StrategySelector(JsoupStrategy jsoupStrategy,
                            ApiStrategy apiStrategy,
                            PlaywrightStrategy playwrightStrategy,
                            DetectionService detectionService) {
        this.jsoupStrategy = jsoupStrategy;
        this.apiStrategy = apiStrategy;
        this.playwrightStrategy = playwrightStrategy;
        this.detectionService = detectionService;
    }

    public ExtractedData extract(String url) {
        log("Starting extraction for: " + url);

        // 1️⃣ Try jsoup first (Default)
        try {
            Document doc = jsoupStrategy.getDocument(url);
            
            if (!detectionService.shouldSwitchFromJsoup(doc)) {
                ExtractedData data = jsoupStrategy.extract(url);
                if (detectionService.isDataQualityGood(data)) {
                    log("Jsoup strategy successful", data);
                    return data;
                }
            }
            log("Jsoup signals low quality or JS-heavy content. Switching...");
        } catch (IOException e) {
            log("Jsoup failed to connect. Switching...");
        }

        // 2️⃣ Try API strategy
        ExtractedData data = apiStrategy.extract(url);
        if (detectionService.isDataQualityGood(data)) {
            log("API strategy successful", data);
            return data;
        }
        log("API strategy found no quality data. Switching to Playwright...");

        // 3️⃣ Final fallback → Playwright
        data = playwrightStrategy.extract(url);
        log("Playwright strategy (Final fallback)", data);

        return data;
    }

    private void log(String message) {
        System.out.println("[StrategySelector] " + message);
    }

    private void log(String strategy, ExtractedData data) {
        System.out.println("[StrategySelector] Strategy: " + strategy +
                " | Success: " + (data != null && data.isSuccess()) +
                " | Score: " + (data != null ? detectionService.calculateScore(data) : 0));
    }
}