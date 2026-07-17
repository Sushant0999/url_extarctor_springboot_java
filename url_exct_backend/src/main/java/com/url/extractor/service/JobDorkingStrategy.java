package com.url.extractor.service;

import com.microsoft.playwright.*;
import com.url.extractor.dto.JobDto;
import com.url.extractor.dto.JobSearchFilter;
import com.url.extractor.utils.MyLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

@Service
public class JobDorkingStrategy {

    @Autowired
    @Qualifier("playwrightSemaphore")
    private Semaphore playwrightSemaphore;

    public List<JobDto> extractDork(JobSearchFilter filter, String platform) {
        List<JobDto> jobs = new ArrayList<>();
        MyLogger.info("JobDorkingStrategy: Executing Playwright Dorking Fallback for platform: " + platform);

        String siteFilter = getSiteFilter(platform);
        if (siteFilter == null) {
            MyLogger.warn("JobDorkingStrategy: No dorking filter available for platform: " + platform);
            return jobs;
        }

        String query = buildDorkQuery(filter, siteFilter);
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        jobs = extractFromBingWithPlaywright(encodedQuery, platform);

        return jobs;
    }

    private List<JobDto> extractFromBingWithPlaywright(String encodedQuery, String platform) {
        List<JobDto> jobs = new ArrayList<>();
        String searchUrl = "https://www.bing.com/search?q=" + encodedQuery;

        try {
            playwrightSemaphore.acquire();
            MyLogger.info("JobDorkingStrategy: Acquired lock, dorking on Bing -> " + searchUrl);

            try (Playwright playwright = Playwright.create();
                 Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setChannel("chrome")
                    .setArgs(List.of("--disable-blink-features=AutomationControlled"))
                 );
                 BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                 );
                 Page page = context.newPage()) {

                page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

                page.navigate(searchUrl, new Page.NavigateOptions().setTimeout(60000).setWaitUntil(com.microsoft.playwright.options.WaitUntilState.LOAD));

                // Wait for results to render
                Locator cards = page.locator(".b_algo");
                try { cards.first().waitFor(new Locator.WaitForOptions().setTimeout(10000)); } catch (Exception ignored) {}

                int count = cards.count();
                MyLogger.info("JobDorkingStrategy: Found " + count + " Bing dork results.");

                for (int i = 0; i < count; i++) {
                    Locator card = cards.nth(i);
                    try {
                        Locator titleEl = card.locator("h2 a").first();
                        if (titleEl.count() == 0) continue;
                        
                        String link = titleEl.getAttribute("href");
                        String title = titleEl.innerText().trim();

                        if (!link.contains(platform)) continue;

                        jobs.add(JobDto.builder()
                                .title(cleanTitle(title, platform))
                                .company(extractCompanyFromTitle(title, platform))
                                .location("Not specified")
                                .link(link)
                                .source(platform + " (Bing Playwright Dork)")
                                .build());
                    } catch (Exception e) {}
                }

            } catch (Exception e) {
                MyLogger.err("JobDorkingStrategy: Bing Playwright extraction failed: " + e.getMessage());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            MyLogger.err("JobDorkingStrategy: Interrupted while waiting for lock: " + e.getMessage());
        } finally {
            playwrightSemaphore.release();
        }

        return jobs;
    }

    private String getSiteFilter(String platform) {
        return switch (platform.toLowerCase()) {
            case "linkedin" -> "site:linkedin.com/jobs/view";
            case "naukri" -> "site:naukri.com/job-listings";
            case "indeed" -> "site:indeed.com/viewjob";
            case "foundit" -> "site:foundit.in/job";
            case "cutshort" -> "site:cutshort.io/job";
            case "internshala" -> "site:internshala.com/internship/detail";
            case "hirist" -> "site:hirist.tech/job";
            default -> null;
        };
    }

    private String buildDorkQuery(JobSearchFilter filter, String siteFilter) {
        StringBuilder query = new StringBuilder(siteFilter);

        if (filter.getQuery() != null && !filter.getQuery().isEmpty()) {
            query.append(" \"").append(filter.getQuery()).append("\"");
        }

        if (filter.getSkills() != null && !filter.getSkills().isEmpty()) {
            for (String skill : filter.getSkills()) {
                query.append(" \"").append(skill).append("\"");
            }
        }

        if (filter.getLocations() != null && !filter.getLocations().isEmpty()) {
            query.append(" \"").append(filter.getLocations().get(0)).append("\"");
        } else if (filter.getCountry() != null && !filter.getCountry().isEmpty()) {
            query.append(" \"").append(filter.getCountry()).append("\"");
        }

        return query.toString().trim();
    }

    private String cleanTitle(String rawTitle, String platform) {
        return rawTitle.replaceAll("(?i)-?\\s*linkedin.*$", "")
                       .replaceAll("(?i)-?\\s*naukri.*$", "")
                       .replaceAll("(?i)-?\\s*indeed.*$", "")
                       .replaceAll("(?i)-?\\s*foundit.*$", "")
                       .replaceAll("(?i)job vacancy.*$", "")
                       .replaceAll("(?i)\\|.*$", "")
                       .trim();
    }

    private String extractCompanyFromTitle(String rawTitle, String platform) {
        if (rawTitle.contains(" at ")) {
            String[] parts = rawTitle.split(" at ");
            if (parts.length > 1) {
                return cleanTitle(parts[1], platform);
            }
        } else if (rawTitle.contains("-")) {
            String[] parts = rawTitle.split("-");
            if (parts.length > 1) {
                return cleanTitle(parts[parts.length-1], platform); 
            }
        }
        return "Unknown";
    }
}
