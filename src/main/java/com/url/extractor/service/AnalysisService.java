package com.url.extractor.service;

import com.url.extractor.dto.ExtractedData;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnalysisService {

    public List<ExtractedData.SeoIssue> performSeoAudit(Document doc, ExtractedData data) {
        List<ExtractedData.SeoIssue> issues = new ArrayList<>();

        // 1. Title Tag
        if (data.getTitle() == null || data.getTitle().isEmpty()) {
            issues.add(new ExtractedData.SeoIssue("Title Tag", "FAIL", "Missing <title> tag."));
        } else if (data.getTitle().length() > 60) {
            issues.add(new ExtractedData.SeoIssue("Title Tag", "WARNING", "Title is too long (" + data.getTitle().length() + " chars). Recommended: < 60."));
        } else {
            issues.add(new ExtractedData.SeoIssue("Title Tag", "PASS", "Title tag is optimized."));
        }

        // 2. Meta Description
        if (data.getDescription() == null || data.getDescription().isEmpty()) {
            issues.add(new ExtractedData.SeoIssue("Meta Description", "FAIL", "Missing meta description."));
        } else {
            issues.add(new ExtractedData.SeoIssue("Meta Description", "PASS", "Meta description present."));
        }

        // 3. H1 Tag
        long h1Count = doc.select("h1").size();
        if (h1Count == 0) {
            issues.add(new ExtractedData.SeoIssue("H1 Header", "FAIL", "No <h1> tag found."));
        } else if (h1Count > 1) {
            issues.add(new ExtractedData.SeoIssue("H1 Header", "WARNING", "Multiple <h1> tags found. Only one is recommended."));
        } else {
            issues.add(new ExtractedData.SeoIssue("H1 Header", "PASS", "Correct single <h1> usage."));
        }

        // 4. Image Alt Texts
        long missingAlt = doc.select("img:not([alt])").size();
        if (missingAlt > 0) {
            issues.add(new ExtractedData.SeoIssue("Image Alt Text", "WARNING", missingAlt + " images are missing 'alt' attributes."));
        } else {
            issues.add(new ExtractedData.SeoIssue("Image Alt Text", "PASS", "All images have alt text."));
        }

        return issues;
    }

    public List<String> detectTechStack(Document doc) {
        List<String> tech = new ArrayList<>();
        String html = doc.html().toLowerCase();

        if (html.contains("wp-content")) tech.add("WordPress");
        if (html.contains("_next/static")) tech.add("Next.js");
        if (html.contains("react")) tech.add("React");
        if (html.contains("vue")) tech.add("Vue.js");
        if (html.contains("shopify")) tech.add("Shopify");
        if (html.contains("bootstrap")) tech.add("Bootstrap");
        if (html.contains("tailwind")) tech.add("TailwindCSS");
        if (html.contains("google-analytics")) tech.add("Google Analytics");

        return tech;
    }

    public List<String> extractColorPalette(Document doc) {
        List<String> colors = new ArrayList<>();
        String styles = doc.select("style").text();
        Pattern colorPattern = Pattern.compile("#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})");
        Matcher matcher = colorPattern.matcher(styles);

        while (matcher.find() && colors.size() < 5) {
            String color = matcher.group();
            if (!colors.contains(color)) {
                colors.add(color);
            }
        }
        
        // Add defaults if nothing found in inline styles
        if (colors.isEmpty()) {
            colors.addAll(List.of("#FFFFFF", "#000000", "#3182CE", "#E53E3E"));
        }
        
        return colors;
    }

    public String generateSummary(ExtractedData data) {
        String content = data.getContent();
        if (content == null || content.length() < 100) return "Content too short for summary.";
        
        // Mock summary for now - in production use an AI API
        return "This page discusses " + data.getTitle() + 
               " and focuses on key topics extracted from its " + (content.split(" ").length) + 
               " words of content. The main areas covered include " + 
               (data.getAnchorTags().size() > 5 ? "navigation to various external resources." : "internal site details.");
    }
}
