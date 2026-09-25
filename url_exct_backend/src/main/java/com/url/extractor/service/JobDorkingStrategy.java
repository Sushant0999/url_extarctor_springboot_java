package com.url.extractor.service;

import com.microsoft.playwright.*;
import com.url.extractor.dto.JobDto;
import com.url.extractor.dto.JobSearchFilter;
import com.url.extractor.utils.MyLogger;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

@Singleton
public class JobDorkingStrategy {

    @Inject
    @Named("playwrightSemaphore")
    private Semaphore playwrightSemaphore;

    public List<JobDto> extractDork(JobSearchFilter filter, String platform) {
        List<JobDto> jobs = new ArrayList<>();
        MyLogger.info("JobDorkingStrategy: Executing Dorking Fallback for platform: " + platform);

        String siteFilter = getSiteFilter(platform);
        if (siteFilter == null) {
            MyLogger.warn("JobDorkingStrategy: No dorking filter available for platform: " + platform);
            return jobs;
        }

        String query = buildDorkQuery(filter, siteFilter);
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String domainFragment = getDomainFragment(platform);

        // Try Google first (more results), fall back to Bing
        jobs = extractWithPlaywright("https://www.google.com/search?q=" + encodedQuery + "&num=20", platform, domainFragment, "div.g");
        if (jobs.isEmpty()) {
            MyLogger.info("JobDorkingStrategy: Google returned 0, trying Bing for: " + platform);
            jobs = extractWithPlaywright("https://www.bing.com/search?q=" + encodedQuery, platform, domainFragment, ".b_algo");
        }

        return jobs;
    }

    private List<JobDto> extractWithPlaywright(String searchUrl, String platform, String domainFragment, String cardSelector) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            playwrightSemaphore.acquire();
            MyLogger.info("JobDorkingStrategy: Acquired lock, dorking -> " + searchUrl);

            try (Playwright playwright = Playwright.create();
                 Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setChannel("chrome")
                    .setArgs(List.of("--disable-blink-features=AutomationControlled", "--no-sandbox"))
                 );
                 BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                 );
                 Page page = context.newPage()) {

                page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
                page.setExtraHTTPHeaders(java.util.Map.of(
                    "Accept-Language", "en-US,en;q=0.9",
                    "Referer", "https://www.google.com/"
                ));

                page.navigate(searchUrl, new Page.NavigateOptions()
                    .setTimeout(60000)
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED));

                // Wait for result cards to appear
                Locator cards = page.locator(cardSelector);
                try { cards.first().waitFor(new Locator.WaitForOptions().setTimeout(10000)); } catch (Exception ignored) {}

                int count = cards.count();
                MyLogger.info("JobDorkingStrategy: Found " + count + " dork results on " + searchUrl);

                for (int i = 0; i < count; i++) {
                    Locator card = cards.nth(i);
                    try {
                        // Get the link
                        Locator linkEl = card.locator("a[href]").first();
                        if (linkEl.count() == 0) continue;

                        String link = linkEl.getAttribute("href");
                        if (link == null || link.isEmpty() || link.startsWith("/search")) continue;

                        // Filter to only links from the target platform domain
                        if (domainFragment != null && !link.contains(domainFragment)) continue;

                        // Get the title text from the heading
                        String rawTitle = "";
                        Locator headingEl = card.locator("h3").first();
                        if (headingEl.count() > 0) rawTitle = headingEl.innerText().trim();
                        if (rawTitle.isEmpty()) rawTitle = linkEl.innerText().trim();
                        if (rawTitle.isEmpty()) continue;

                        String title = cleanTitle(rawTitle, platform);
                        String company = extractCompanyFromTitle(rawTitle, platform);

                        // Try to get description/snippet for additional context
                        String snippet = "";
                        Locator descEl = card.locator("div[style], [data-sncf], .VwiC3b, .b_caption p").first();
                        if (descEl.count() > 0) snippet = descEl.innerText().trim();

                        // If company is Unknown, try to extract from snippet
                        if (company.equals("Unknown") && !snippet.isEmpty()) {
                            company = extractCompanyFromTitle(snippet, platform);
                        }

                        // Fix relative links
                        if (link.startsWith("//")) link = "https:" + link;

                        jobs.add(JobDto.builder()
                                .title(title)
                                .company(company)
                                .location("Not specified")
                                .link(link)
                                .source(platform + " (Dork)")
                                .build());
                    } catch (Exception e) {
                        MyLogger.warn("JobDorkingStrategy: Card " + i + " failed: " + e.getMessage());
                    }
                }

            } catch (Exception e) {
                MyLogger.err("JobDorkingStrategy: Playwright dorking failed on " + searchUrl + ": " + e.getMessage());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            MyLogger.err("JobDorkingStrategy: Interrupted while waiting for semaphore: " + e.getMessage());
        } finally {
            playwrightSemaphore.release();
        }

        return jobs;
    }

    private String getSiteFilter(String platform) {
        return switch (platform.toLowerCase()) {
            case "linkedin"    -> "site:linkedin.com/jobs/view";
            case "naukri"      -> "site:naukri.com/job-listings";
            case "indeed"      -> "site:in.indeed.com/viewjob OR site:indeed.com/viewjob";
            case "foundit"     -> "site:foundit.in/job";
            case "cutshort"    -> "site:cutshort.io/jobs";
            case "internshala" -> "site:internshala.com/internship/detail OR site:internshala.com/jobs/detail";
            case "hirist"      -> "site:hirist.tech/j";
            case "shine"       -> "site:shine.com/jobs";
            default            -> null;
        };
    }

    private String getDomainFragment(String platform) {
        return switch (platform.toLowerCase()) {
            case "linkedin"    -> "linkedin.com";
            case "naukri"      -> "naukri.com";
            case "indeed"      -> "indeed.com";
            case "foundit"     -> "foundit.in";
            case "cutshort"    -> "cutshort.io";
            case "internshala" -> "internshala.com";
            case "hirist"      -> "hirist.tech";
            case "shine"       -> "shine.com";
            default            -> null;
        };
    }

    private String buildDorkQuery(JobSearchFilter filter, String siteFilter) {
        StringBuilder query = new StringBuilder(siteFilter);

        if (filter.getQuery() != null && !filter.getQuery().isEmpty()) {
            query.append(" \"").append(filter.getQuery()).append("\"");
        }

        if (filter.getSkills() != null && !filter.getSkills().isEmpty()) {
            // Only append first 2 skills to keep query focused
            filter.getSkills().stream().limit(2).forEach(skill ->
                query.append(" \"").append(skill).append("\"")
            );
        }

        if (filter.getLocations() != null && !filter.getLocations().isEmpty()) {
            query.append(" \"").append(filter.getLocations().get(0)).append("\"");
        } else if (filter.getCountry() != null && !filter.getCountry().isEmpty()) {
            // Map ISO code to country name for better search results
            String country = filter.getCountry().equalsIgnoreCase("in") ? "India" : filter.getCountry();
            query.append(" ").append(country);
        }

        return query.toString().trim();
    }

    private String cleanTitle(String rawTitle, String platform) {
        return rawTitle
            .replaceAll("(?i)-?\\s*linkedin.*$", "")
            .replaceAll("(?i)-?\\s*naukri.*$", "")
            .replaceAll("(?i)-?\\s*indeed.*$", "")
            .replaceAll("(?i)-?\\s*foundit.*$", "")
            .replaceAll("(?i)-?\\s*cutshort.*$", "")
            .replaceAll("(?i)-?\\s*internshala.*$", "")
            .replaceAll("(?i)-?\\s*hirist.*$", "")
            .replaceAll("(?i)-?\\s*shine.*$", "")
            .replaceAll("(?i)job vacancy.*$", "")
            .replaceAll("(?i)\\|.*$", "")
            .replaceAll("(?i)\\s+jobs?\\s*$", "")
            .trim();
    }

    private String extractCompanyFromTitle(String rawTitle, String platform) {
        // "Software Engineer at Google" pattern
        if (rawTitle.contains(" at ")) {
            String[] parts = rawTitle.split(" at ", 2);
            if (parts.length > 1) return cleanTitle(parts[1], platform);
        }
        // "Software Engineer - Google" pattern
        if (rawTitle.contains(" - ")) {
            String[] parts = rawTitle.split(" - ");
            if (parts.length > 1) return cleanTitle(parts[parts.length - 1], platform);
        }
        return "Unknown";
    }
}
