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

    // Original fallback endpoint
    @PostMapping("/search")
    @Operation(summary = "Search for jobs", description = "Fallback search endpoint across selected platforms in filter.")
    public ResponseEntity<List<JobDto>> searchJobs(@RequestBody JobSearchFilter filter) {
        return processSearchRequest(filter);
    }

    // Explicit Mappings as Requested
    @PostMapping("/search/indeed")
    public ResponseEntity<List<JobDto>> searchIndeed(@RequestBody JobSearchFilter filter) { return searchSpecificPlatform("indeed", filter); }

    @PostMapping("/search/linkedin")
    public ResponseEntity<List<JobDto>> searchLinkedin(@RequestBody JobSearchFilter filter) { return searchSpecificPlatform("linkedin", filter); }

    @PostMapping("/search/naukri")
    public ResponseEntity<List<JobDto>> searchNaukri(@RequestBody JobSearchFilter filter) { return searchSpecificPlatform("naukri", filter); }

    @PostMapping("/search/cutshort")
    public ResponseEntity<List<JobDto>> searchCutshort(@RequestBody JobSearchFilter filter) { return searchSpecificPlatform("cutshort", filter); }

    @PostMapping("/search/foundit")
    public ResponseEntity<List<JobDto>> searchFoundit(@RequestBody JobSearchFilter filter) { return searchSpecificPlatform("foundit", filter); }

    @PostMapping("/search/internshala")
    public ResponseEntity<List<JobDto>> searchInternshala(@RequestBody JobSearchFilter filter) { return searchSpecificPlatform("internshala", filter); }

    @PostMapping("/search/shine")
    public ResponseEntity<List<JobDto>> searchShine(@RequestBody JobSearchFilter filter) { return searchSpecificPlatform("shine", filter); }

    @PostMapping("/search/hirist")
    public ResponseEntity<List<JobDto>> searchHirist(@RequestBody JobSearchFilter filter) { return searchSpecificPlatform("hirist", filter); }

    private ResponseEntity<List<JobDto>> searchSpecificPlatform(String platform, JobSearchFilter filter) {
        MyLogger.info("JobController: Direct API Call to Specific Platform Backend -> " + platform.toUpperCase());
        filter.setPlatforms(List.of(platform)); // Force the array securely
        return processSearchRequest(filter);
    }

    private ResponseEntity<List<JobDto>> processSearchRequest(JobSearchFilter filter) {
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
