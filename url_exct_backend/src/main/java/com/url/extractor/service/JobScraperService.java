package com.url.extractor.service;

import com.url.extractor.dto.JobDto;
import com.url.extractor.dto.JobSearchFilter;
import com.url.extractor.utils.MyLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class JobScraperService {

    @Autowired
    private PlatformUrlBuilder urlBuilder;

    @Autowired
    private JobJsoupStrategy jsoupStrategy;

    @Autowired
    private JobPlaywrightStrategy playwrightStrategy;

    public List<JobDto> searchJobs(JobSearchFilter filter) {
        List<String> platforms = filter.getPlatforms();
        if (platforms == null || platforms.isEmpty()) {
            platforms = List.of("indeed");
        }

        MyLogger.info("JobScraperService: Starting parallel search across platforms: " + platforms);

        // Using CompletableFuture for internal backend threading
        List<CompletableFuture<List<?>>> futures = platforms.stream()
            .map(platform -> CompletableFuture.supplyAsync(() -> {
                String url = urlBuilder.buildUrl(filter, platform);
                List<JobDto> platformResults = new ArrayList<>();

                // 1. Try Jsoup
                try {
                    platformResults = jsoupStrategy.extract(url);
                    if (!platformResults.isEmpty()) {
                        MyLogger.info("JobScraperService: Jsoup success for " + platform + " (" + platformResults.size() + " jobs)");
                        return platformResults;
                    }
                } catch (Exception e) {
                    MyLogger.err("JobScraperService: Jsoup failed for " + platform + ": " + e.getMessage());
                }

                // 2. Playwright Fallback
                try {
                    return playwrightStrategy.extract(url);
                } catch (Exception e) {
                    MyLogger.err("JobScraperService: Playwright failed for " + platform + ": " + e.getMessage());
                    return new ArrayList<>();
                }
            }))
            .toList();

        // Wait for all platforms to finish and collect results
        List<JobDto> allResults = futures.stream()
            .map(f -> (List<JobDto>) f.join())
            .flatMap(java.util.List::stream)
            .collect(java.util.stream.Collectors.toList());

        return deduplicate(allResults);
    }

    private List<JobDto> deduplicate(List<JobDto> jobs) {
        java.util.Map<String, JobDto> uniqueJobs = new java.util.LinkedHashMap<>();

        for (JobDto job : jobs) {
            String title = normalize(job.getTitle());
            String company = normalize(job.getCompany());
            String key = title + "|" + company;

            if (uniqueJobs.containsKey(key)) {
                JobDto existing = uniqueJobs.get(key);
                // Merge sources if not already present
                if (!existing.getSource().toLowerCase().contains(job.getSource().toLowerCase())) {
                    existing.setSource(existing.getSource() + ", " + job.getSource());
                }
                // Keep the better link (e.g. LinkedIn over others if possible, or just keep first)
                if (existing.getLink() == null) existing.setLink(job.getLink());
                // Merge other fields if missing
                if (existing.getSalary() == null) existing.setSalary(job.getSalary());
                if (existing.getLocation() == null) existing.setLocation(job.getLocation());
            } else {
                uniqueJobs.put(key, job);
            }
        }
        return new ArrayList<>(uniqueJobs.values());
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("\\(.*?\\)", "") // Remove text in parentheses
                .replaceAll("(?i)pvt\\.?\\s*ltd\\.?", "") // Remove Pvt Ltd
                .replaceAll("(?i)ltd\\.?", "") // Remove Ltd
                .replaceAll("(?i)inc\\.?", "") // Remove Inc
                .replaceAll("[^a-z0-9]", "") // Remove all non-alphanumeric
                .trim();
    }
}
