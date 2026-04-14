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

        // Wait for all platforms to finish and collect results safely
        List<JobDto> allResults = futures.stream()
            .map(f -> {
                try {
                    return (List<JobDto>) f.join();
                } catch (Exception e) {
                    MyLogger.err("JobScraperService: A platform thread crashed during execution: " + e.getMessage());
                    return new ArrayList<JobDto>();
                }
            })
            .flatMap(java.util.List::stream)
            .collect(java.util.stream.Collectors.toList());

        // Filter out completely irrelevant algorithmic garbage (e.g., Sponsored jobs from different industries)
        List<JobDto> relevantJobs = filterIrrelevantJobs(allResults, filter);

        return deduplicate(relevantJobs);
    }

    private List<JobDto> filterIrrelevantJobs(List<JobDto> jobs, JobSearchFilter filter) {
        String query = filter.getQuery() != null ? filter.getQuery().toLowerCase() : "";
        List<String> skills = filter.getSkills() != null ? filter.getSkills().stream().map(String::toLowerCase).toList() : new ArrayList<>();
        String expLevel = filter.getExperienceLevel() != null ? filter.getExperienceLevel().toLowerCase() : "";
        
        if (query.isEmpty() && skills.isEmpty() && expLevel.isEmpty()) return jobs;

        List<String> matchTokens = new ArrayList<>();
        for (String w : query.split("[^a-z0-9]+")) {
            if (w.length() > 2) matchTokens.add(w); 
        }
        for (String s : skills) {
             for (String w : s.split("[^a-z0-9]+")) {
                 if (w.length() > 2) matchTokens.add(w);
             }
        }

        return jobs.stream().filter(job -> {
            String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
            String location = job.getLocation() != null ? job.getLocation().toLowerCase() : "";
            String iso = filter.getCountry() != null ? filter.getCountry().toLowerCase() : "in";

            // 1. Geographic Platform Guard
            if (!iso.equals("in") && !iso.equals("india")) {
                if (location.contains("india") || location.contains("chennai") || location.contains("mumbai") ||
                    location.contains("pune") || location.contains("delhi") || location.contains("noida") ||
                    location.contains("gurugram") || location.contains("gurgaon") || location.contains("bengaluru") ||
                    location.contains("bangalore") || location.contains("hyderabad") || location.contains("kochi") ||
                    location.contains("kolkata") || location.contains("ahmedabad") || location.contains("chandigarh")) {
                    MyLogger.info("JobScraperService: Dropped hallucinated Indian job for foreign query -> " + job.getTitle());
                    return false;
                }
            }
            
            // 2. Experience Level Strict Clamping
            if (expLevel.equals("entry_level")) {
                if (title.contains("senior") || title.matches(".*\\bsr\\.?\\b.*") || title.contains("lead") || title.contains("architect") || title.contains("principal") || title.contains("manager") || title.contains("mid")) {
                    MyLogger.info("JobScraperService: Dropped incompatible Senior/Mid role for Entry Level search -> " + job.getTitle());
                    return false;
                }
            } else if (expLevel.equals("mid_level")) {
                if (title.contains("senior") || title.matches(".*\\bsr\\.?\\b.*") || title.contains("architect") || title.contains("principal") || title.contains("intern") || title.contains("fresher") || title.contains("trainee") || title.contains("student")) {
                    MyLogger.info("JobScraperService: Dropped incompatible level role for Mid Level search -> " + job.getTitle());
                    return false;
                }
            } else if (expLevel.equals("senior_level")) {
                if (title.contains("intern") || title.contains("fresher") || title.contains("trainee") || title.contains("junior") || title.matches(".*\\bjr\\.?\\b.*")) {
                    MyLogger.info("JobScraperService: Dropped incompatible Junior role for Senior Level search -> " + job.getTitle());
                    return false;
                }
            }

            // 3. Negative Hierarchy Match (Unrequested Manager/VP roles)
            if ((title.contains("manager") && !matchTokens.contains("manager")) ||
                (title.contains("director") && !matchTokens.contains("director")) ||
                (title.contains("vp ") && !matchTokens.contains("vp")) ||
                (title.contains("head of ") && !matchTokens.contains("head"))) {
                MyLogger.info("JobScraperService: Dropped unrequested hierarchy jump -> " + job.getTitle());
                return false;
            }

            // If we have no query tokens to match, but we passed all negative filters, we survived.
            if (matchTokens.isEmpty()) return true;
            
            // 4. Positive Token Relevance Check
            for (String token : matchTokens) {
                if (title.contains(token)) {
                    MyLogger.info("JobScraperService: Accepted job (token match) -> " + job.getTitle());
                    return true;
                }
            }
            
            // Allow synonymous cross-matching common in tech
            if ((matchTokens.contains("developer") && title.contains("engineer")) || 
                (matchTokens.contains("engineer") && title.contains("developer")) ||
                (matchTokens.contains("software") && (title.contains("programmer") || title.contains("coder")))) {
                MyLogger.info("JobScraperService: Accepted job (synonym match) -> " + job.getTitle());
                return true;
            }
            
            MyLogger.info("JobScraperService: Dropped irrelevant job (no token match) -> " + job.getTitle());
            return false;
        }).collect(java.util.stream.Collectors.toList());
    }

    private List<JobDto> deduplicate(List<JobDto> jobs) {
        java.util.Map<String, JobDto> uniqueJobs = new java.util.LinkedHashMap<>();

        for (JobDto job : jobs) {
            String title = normalize(job.getTitle());
            String company = normalize(job.getCompany());
            String key = title + "|" + company;

            if (uniqueJobs.containsKey(key)) {
                JobDto existing = uniqueJobs.get(key);
                // Merge sources if not already present, safely handling null sources
                String currentSource = existing.getSource() != null ? existing.getSource() : "";
                String newSource = job.getSource() != null ? job.getSource() : "Unknown";
                
                if (!currentSource.toLowerCase().contains(newSource.toLowerCase())) {
                    existing.setSource(currentSource.isEmpty() ? newSource : currentSource + ", " + newSource);
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
