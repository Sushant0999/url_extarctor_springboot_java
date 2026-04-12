package com.url.extractor.controller;

import com.url.extractor.dto.JobDto;
import com.url.extractor.dto.JobSearchFilter;
import com.url.extractor.service.JobScraperService;
import com.url.extractor.utils.MyLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "${app.allowed.origins}", allowedHeaders = "*", maxAge = 3600L, methods = { RequestMethod.GET, RequestMethod.OPTIONS, RequestMethod.POST })
@Tag(name = "Job Search API", description = "Endpoints for searching and scraping job postings with filters.")
public class JobController {

    @Autowired
    private JobScraperService jobScraperService;

    // Simple in-memory cache for job results to enhance performance as requested
    private final Map<String, List<JobDto>> jobCache = new ConcurrentHashMap<>();

    @PostMapping("/search")
    @Operation(summary = "Search for jobs", description = "Scrapes job listings from Indeed based on provided filters (query, location, skills, etc.).")
    public ResponseEntity<List<JobDto>> searchJobs(@RequestBody JobSearchFilter filter) {
        String cacheKey = generateCacheKey(filter);
        
        if (jobCache.containsKey(cacheKey)) {
            MyLogger.info("JobController: Returning cached results for query: " + filter.getQuery());
            return ResponseEntity.ok(jobCache.get(cacheKey));
        }

        List<JobDto> results = jobScraperService.searchJobs(filter);
        
        if (!results.isEmpty()) {
            jobCache.put(cacheKey, results);
        }

        return ResponseEntity.ok(results);
    }

    private String generateCacheKey(JobSearchFilter filter) {
        return String.format("%s_%s_%s_%s_%s_%s_%s_%s_%s_%s_%s", 
            filter.getQuery(), 
            filter.getLocations(), 
            filter.getJobType(), 
            filter.getDistance(), 
            filter.getSkills() != null ? String.join(",", filter.getSkills()) : "",
            filter.getAdditionalKeywords(),
            filter.getWorkMode(),
            filter.getExperienceLevel(),
            filter.getCountry(),
            filter.getPlatforms(),
            filter.getCompanies());
    }

    @GetMapping("/clear-cache")
    @Operation(summary = "Clear job search cache", description = "Clears all cached job search results.")
    public ResponseEntity<String> clearCache() {
        jobCache.clear();
        return ResponseEntity.ok("Cache cleared successfully.");
    }
}
