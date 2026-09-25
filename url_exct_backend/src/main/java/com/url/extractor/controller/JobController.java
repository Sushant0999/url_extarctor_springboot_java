package com.url.extractor.controller;

import com.url.extractor.dto.JobDto;
import com.url.extractor.dto.JobSearchFilter;
import com.url.extractor.service.JobScraperService;
import com.url.extractor.utils.MyLogger;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller("/api/jobs")
@Tag(name = "Job Search API", description = "Endpoints for searching and scraping job postings with filters.")
public class JobController {

    @Inject
    private JobScraperService jobScraperService;

    // Simple in-memory cache for job results to enhance performance as requested
    private final Map<String, List<JobDto>> jobCache = new ConcurrentHashMap<>();

    // Original fallback endpoint
    @Post("/search")
    @Operation(summary = "Search for jobs", description = "Fallback search endpoint across selected platforms in filter.")
    public HttpResponse<List<JobDto>> searchJobs(@Body JobSearchFilter filter) {
        return processSearchRequest(filter);
    }

    // Explicit Mappings
    @Post("/search/indeed")
    public HttpResponse<List<JobDto>> searchIndeed(@Body JobSearchFilter filter) { return searchSpecificPlatform("indeed", filter); }

    @Post("/search/linkedin")
    public HttpResponse<List<JobDto>> searchLinkedin(@Body JobSearchFilter filter) { return searchSpecificPlatform("linkedin", filter); }

    @Post("/search/naukri")
    public HttpResponse<List<JobDto>> searchNaukri(@Body JobSearchFilter filter) { return searchSpecificPlatform("naukri", filter); }

    @Post("/search/cutshort")
    public HttpResponse<List<JobDto>> searchCutshort(@Body JobSearchFilter filter) { return searchSpecificPlatform("cutshort", filter); }

    @Post("/search/foundit")
    public HttpResponse<List<JobDto>> searchFoundit(@Body JobSearchFilter filter) { return searchSpecificPlatform("foundit", filter); }

    @Post("/search/internshala")
    public HttpResponse<List<JobDto>> searchInternshala(@Body JobSearchFilter filter) { return searchSpecificPlatform("internshala", filter); }

    @Post("/search/shine")
    public HttpResponse<List<JobDto>> searchShine(@Body JobSearchFilter filter) { return searchSpecificPlatform("shine", filter); }

    @Post("/search/hirist")
    public HttpResponse<List<JobDto>> searchHirist(@Body JobSearchFilter filter) { return searchSpecificPlatform("hirist", filter); }

    private HttpResponse<List<JobDto>> searchSpecificPlatform(String platform, JobSearchFilter filter) {
        MyLogger.info("JobController: Direct API Call to Specific Platform Backend -> " + platform.toUpperCase());
        filter.setPlatforms(List.of(platform)); // Force the array securely
        return processSearchRequest(filter);
    }

    private HttpResponse<List<JobDto>> processSearchRequest(JobSearchFilter filter) {
        String cacheKey = generateCacheKey(filter);
        
        if (jobCache.containsKey(cacheKey)) {
            MyLogger.info("JobController: Returning cached results for query: " + filter.getQuery());
            return HttpResponse.ok(jobCache.get(cacheKey));
        }

        List<JobDto> results = jobScraperService.searchJobs(filter);
        
        if (!results.isEmpty()) {
            jobCache.put(cacheKey, results);
        }

        return HttpResponse.ok(results);
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

    @Get("/clear-cache")
    @Operation(summary = "Clear job search cache", description = "Clears all cached job search results.")
    public HttpResponse<String> clearCache() {
        jobCache.clear();
        return HttpResponse.ok("Cache cleared successfully.");
    }
}
