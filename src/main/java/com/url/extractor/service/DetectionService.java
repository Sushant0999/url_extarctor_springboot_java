package com.url.extractor.service;

import com.url.extractor.dto.ExtractedData;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class DetectionService {

    public boolean shouldSwitchFromJsoup(Document document) {
        if (document == null) return true;

        // Rule 1: Empty Body Check
        if (document.body() == null || document.body().text().isEmpty()) {
            return true;
        }

        // Rule 2: Low Content Heuristic
        if (document.text().length() < 500) {
            return true;
        }

        // Rule 3: Missing Meta Tags
        String title = document.title();
        String description = document.select("meta[name=description]").attr("content");
        if ((title == null || title.isEmpty()) && (description == null || description.isEmpty())) {
            return true;
        }

        // Rule 4: JS App Detection
        if (document.getElementById("root") != null || document.getElementById("app") != null) {
            return true;
        }

        // Rule 5: Script Dominance
        int scriptTags = document.select("script").size();
        int htmlTags = document.getAllElements().size() - scriptTags;
        if (scriptTags > htmlTags) {
            return true;
        }

        return false;
    }

    public int calculateScore(ExtractedData data) {
        if (data == null) return 0;
        
        int score = 0;
        if (data.getTitle() != null && !data.getTitle().isEmpty()) score += 40;
        if (data.getDescription() != null && !data.getDescription().isEmpty()) score += 30;
        if (data.getContent() != null && data.getContent().length() > 1000) score += 30;
        
        return score;
    }

    public boolean isDataQualityGood(ExtractedData data) {
        return calculateScore(data) >= 50;
    }
}
