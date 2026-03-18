package com.url.extractor.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedData {

    // Original fields
    private String title;
    private String description;
    private String content;
    private boolean success;

    // Enhanced Feature Fields
    private String screenshotBase64;
    private List<SeoIssue> seoIssues;
    private List<String> colorPalette;
    private List<String> techStack;
    private List<LinkStatus> brokenLinks;
    private String summary;
    
    // Requested fields (legacy support)
    private int id;
    private String topics;
    private String category;
    private String baseUrl;
    private List<String> anchorTags;
    private String baseString;
    private List<String> keyword;
    private List<String> imageUrls;
    private List<String> videoUrls;
    private List<byte[]> images;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SeoIssue {
        private String title;
        private String status; // PASS, FAIL, WARNING
        private String description;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LinkStatus {
        private String url;
        private int statusCode;
        private boolean isBroken;
    }

    public boolean isValid() {
        return (title != null && !title.isEmpty()) ||
                (description != null && !description.isEmpty()) ||
                (content != null && content.length() > 300);
    }
}
