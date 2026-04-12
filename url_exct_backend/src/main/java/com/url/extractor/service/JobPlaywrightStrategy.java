package com.url.extractor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.url.extractor.dto.JobDto;
import com.url.extractor.helper.JobExtractionStrategy;
import com.url.extractor.utils.MyLogger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Service
public class JobPlaywrightStrategy implements JobExtractionStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Playwright consumes ~200-300MB per instance. We restrict this to 1 concurrent instance to prevent OOM on 512MB limits.
    private final Semaphore playwrightSemaphore = new Semaphore(1);

    @Override
    public List<JobDto> extract(String url) {
        MyLogger.info("JobPlaywrightStrategy: Queuing for extraction from " + url);
        
        try {
            // Wait for the lock to acquire (limit 1 global concurrent playwright browser)
            playwrightSemaphore.acquire();
            MyLogger.info("JobPlaywrightStrategy: Acquired lock, extracting from " + url);

            // Playwright objects are NOT thread-safe. We must create a new instance per thread
            // to support parallel searching from the frontend without "__adopt__" errors.
            try (Playwright playwright = Playwright.create();
                 Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                 BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                 );
                 Page page = context.newPage()) {

                page.setExtraHTTPHeaders(Map.of(
                    "Accept-Language", "en-US,en;q=0.9",
                    "Referer", "https://www.google.com/"
                ));

                page.navigate(url, new Page.NavigateOptions().setTimeout(60000).setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED));
                
                // Allow some time for Dynamic data to fully load
                page.waitForTimeout(6000);

                if (url.contains("linkedin.com")) {
                    return extractFromLinkedIn(page);
                } else if (url.contains("naukri.com")) {
                    return extractFromNaukri(page);
                } else if (url.contains("cutshort.io")) {
                    return extractFromCutshort(page);
                } else if (url.contains("foundit.in")) {
                    return extractFromFoundit(page);
                } else if (url.contains("internshala.com")) {
                    return extractFromInternshala(page);
                } else if (url.contains("shine.com")) {
                    return extractFromShine(page);
                } else if (url.contains("hirist.com")) {
                    return extractFromHirist(page);
                }

                List<JobDto> jobs = extractFromMosaicData(page);
                if (jobs.isEmpty()) {
                    jobs = extractWithSelectors(page);
                }

                return jobs;

            } catch (Exception e) {
                MyLogger.err("JobPlaywrightStrategy: Extraction error for " + url + ": " + e.getMessage());
                return new ArrayList<>();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            MyLogger.err("JobPlaywrightStrategy: Interrupted while waiting for lock: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            playwrightSemaphore.release();
        }
    }

    private List<JobDto> extractFromNaukri(Page page) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            // Naukri uses article.jobTuple or .cust-job-tuple
            Locator cards = page.locator("article.jobTuple, .cust-job-tuple, [class*='jobTuple']");
            int count = cards.count();
            MyLogger.info("JobPlaywrightStrategy: Found " + count + " Naukri job cards.");

            for (int i = 0; i < count; i++) {
                Locator card = cards.nth(i);
                try {
                    String title = card.locator("a.title, .title").innerText().trim();
                    String company = card.locator("a.subTitle, .comp-name, .company-name").innerText().trim();
                    String location = card.locator(".locWraper, .loc-wrap").innerText().trim();
                    String link = card.locator("a.title, .title").getAttribute("href");
                    String date = card.locator(".job-post-day, .posted-day").innerText().trim();

                    jobs.add(JobDto.builder()
                            .title(title)
                            .company(company)
                            .location(location)
                            .link(link)
                            .datePosted(date)
                            .source("Naukri")
                            .build());
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            MyLogger.err("JobPlaywrightStrategy: Naukri extraction failed: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobDto> extractFromCutshort(Page page) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            // Cutshort cards often change; using multiple common patterns
            Locator cards = page.locator("[data-testid='job-listing-card'], .job-listing-card, .job-card-wrapper, .job-card");
            int count = cards.count();
            MyLogger.info("JobPlaywrightStrategy: Found " + count + " Cutshort job cards.");

            for (int i = 0; i < count; i++) {
                Locator card = cards.nth(i);
                try {
                    String title = card.locator(".job-title, [class*='jobTitle']").innerText().trim();
                    String company = card.locator(".company-name, [class*='companyName']").innerText().trim();
                    String link = card.locator("a").first().getAttribute("href");
                    if (link != null && !link.startsWith("http")) link = "https://cutshort.io" + link;

                    jobs.add(JobDto.builder()
                            .title(title)
                            .company(company)
                            .link(link)
                            .source("Cutshort")
                            .build());
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            MyLogger.err("JobPlaywrightStrategy: Cutshort extraction failed: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobDto> extractFromFoundit(Page page) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            Locator cards = page.locator(".srpResultCard, [class*='srpResultCard']");
            int count = cards.count();
            MyLogger.info("JobPlaywrightStrategy: Found " + count + " Foundit job cards.");

            for (int i = 0; i < count; i++) {
                Locator card = cards.nth(i);
                try {
                    String title = card.locator(".jobTitle, [class*='jobTitle']").innerText().trim();
                    String company = card.locator(".companyName, [class*='companyName']").innerText().trim();
                    String location = card.locator(".location").innerText().trim();
                    String link = card.locator("a").first().getAttribute("href");
                    if (link != null && !link.startsWith("http")) link = "https://www.foundit.in" + link;

                    jobs.add(JobDto.builder()
                            .title(title)
                            .company(company)
                            .location(location)
                            .link(link)
                            .source("Foundit")
                            .build());
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            MyLogger.err("JobPlaywrightStrategy: Foundit extraction failed: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobDto> extractFromInternshala(Page page) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            Locator cards = page.locator(".container-fluid.individual_internship");
            int count = cards.count();
            MyLogger.info("JobPlaywrightStrategy: Found " + count + " Internshala internship cards.");

            for (int i = 0; i < count; i++) {
                Locator card = cards.nth(i);
                try {
                    String title = card.locator(".heading_4_5").innerText().trim();
                    String company = card.locator(".heading_6").innerText().trim();
                    String location = card.locator(".location_link").innerText().trim();
                    String link = card.locator("a").first().getAttribute("href");
                    if (link != null && !link.startsWith("http")) link = "https://internshala.com" + link;

                    jobs.add(JobDto.builder()
                            .title(title)
                            .company(company)
                            .location(location)
                            .link(link)
                            .source("Internshala")
                            .build());
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            MyLogger.err("JobPlaywrightStrategy: Internshala extraction failed: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobDto> extractFromShine(Page page) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            // Shine uses .jobCard as each listing card
            Locator cards = page.locator(".jobCard, [class*='jobCard']:not([class*='jobCardSkeleton'])");
            int count = cards.count();
            MyLogger.info("JobPlaywrightStrategy: Found " + count + " Shine job cards.");

            int success = 0;
            for (int i = 0; i < count; i++) {
                Locator card = cards.nth(i);
                try {
                    // Try multiple title selectors in priority order
                    String title = safeText(card, "h2, h3, .jobTitle, [class*='jobTitle'], [class*='title'], [itemprop='title']");
                    if (title.isEmpty()) continue; // skip skeleton/empty cards

                    String company = safeText(card, ".companyName, [class*='company'], [class*='Company'], [itemprop='name']");
                    String location = safeText(card, ".location, [class*='location'], [class*='Location'], [class*='loc']");
                    String salary = safeText(card, ".salaryLpa, [class*='salary'], [class*='Salary'], [class*='ctc']");

                    // Links
                    String link = null;
                    try { link = card.locator("a").first().getAttribute("href"); } catch (Exception ignored) {}
                    if (link != null && !link.startsWith("http")) link = "https://www.shine.com" + link;

                    jobs.add(JobDto.builder()
                            .title(title)
                            .company(company.isEmpty() ? "Unknown" : company)
                            .location(location.isEmpty() ? null : location)
                            .salary(salary.isEmpty() ? null : salary)
                            .link(link)
                            .source("Shine")
                            .build());
                    success++;
                } catch (Exception e) {
                    MyLogger.warn("JobPlaywrightStrategy: Shine card " + i + " failed: " + e.getMessage());
                }
            }
            MyLogger.info("JobPlaywrightStrategy: Shine extracted " + success + "/" + count + " jobs.");
        } catch (Exception e) {
            MyLogger.err("JobPlaywrightStrategy: Shine extraction failed: " + e.getMessage());
        }
        return jobs;
    }

    /** Safely tries multiple comma-separated selectors and returns the first non-empty text */
    private String safeText(Locator parent, String selectors) {
        try {
            Locator el = parent.locator(selectors).first();
            if (el.count() > 0) return el.innerText().trim();
        } catch (Exception ignored) {}
        return "";
    }

    private List<JobDto> extractFromHirist(Page page) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            Locator cards = page.locator(".job-card, .job-item, [class*='jobCard']");
            int count = cards.count();
            MyLogger.info("JobPlaywrightStrategy: Found " + count + " Hirist job cards.");

            for (int i = 0; i < count; i++) {
                Locator card = cards.nth(i);
                try {
                    String title = card.locator(".job-title, h3").innerText().trim();
                    String company = card.locator(".company-name, .recruiter-name").innerText().trim();
                    String link = card.locator("a").first().getAttribute("href");
                    if (link != null && !link.startsWith("http")) link = "https://www.hirist.com" + link;

                    jobs.add(JobDto.builder()
                            .title(title)
                            .company(company)
                            .link(link)
                            .source("Hirist")
                            .build());
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            MyLogger.err("JobPlaywrightStrategy: Hirist extraction failed: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobDto> extractFromLinkedIn(Page page) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            // LinkedIn public cards often use .base-card or similar
            Locator cards = page.locator(".base-card, .base-search-card, .job-search-card, [data-entity-urn^='urn:li:jobPost']");
            int count = cards.count();
            MyLogger.info("JobPlaywrightStrategy: Found " + count + " LinkedIn job cards.");

            for (int i = 0; i < count; i++) {
                Locator card = cards.nth(i);
                try {
                    String title = card.locator(".base-search-card__title, .job-search-card__title").innerText().trim();
                    String company = card.locator(".base-search-card__subtitle, .job-search-card__subtitle").innerText().trim();
                    String location = card.locator(".job-search-card__location").innerText().trim();
                    String link = card.locator("a.base-card__full-link, a.job-search-card__link").getAttribute("href");
                    String date = card.locator("time").getAttribute("datetime");

                    jobs.add(JobDto.builder()
                            .title(title)
                            .company(company)
                            .location(location)
                            .link(link)
                            .datePosted(date)
                            .source("LinkedIn")
                            .build());
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            MyLogger.err("JobPlaywrightStrategy: LinkedIn extraction failed: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobDto> extractFromMosaicData(Page page) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            String scriptContent = (String) page.evaluate("() => document.getElementById('mosaic-data') ? document.getElementById('mosaic-data').textContent : null");

            if (scriptContent != null) {
                // Indeed sometimes wraps the JSON in window.mosaic.initialData = { ... };
                String jsonPart = scriptContent;
                if (scriptContent.contains("window.mosaic.initialData")) {
                    int start = scriptContent.indexOf("{");
                    int end = scriptContent.lastIndexOf("}");
                    if (start != -1 && end != -1 && end > start) {
                        jsonPart = scriptContent.substring(start, end + 1);
                    }
                }

                JsonNode root = objectMapper.readTree(jsonPart);
                JsonNode results = root.findValue("results");

                if (results != null && results.isArray()) {
                    for (JsonNode node : results) {
                        jobs.add(JobDto.builder()
                                .title(node.path("title").asText())
                                .company(node.path("company").asText())
                                .location(node.path("formattedLocation").asText())
                                .link("https://www.indeed.com/viewjob?jk=" + node.path("jobkey").asText())
                                .salary(node.path("salaryText").path("text").asText("Not disclosed"))
                                .datePosted(node.path("formattedRelativeTime").asText())
                                .source("Indeed (Playwright)")
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            MyLogger.err("JobPlaywrightStrategy: Mosaic parsing error: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobDto> extractWithSelectors(Page page) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            Locator cards = page.locator(".job_seen_beacon");
            int count = cards.count();

            for (int i = 0; i < count; i++) {
                Locator card = cards.nth(i);
                try {
                    String title = card.locator("h2.jobTitle").innerText();
                    String company = card.locator("[data-testid='company-name']").innerText();
                    String location = card.locator("[data-testid='text-location']").innerText();
                    String link = card.locator("a.jcs-JobTitle").getAttribute("href");

                    jobs.add(JobDto.builder()
                            .title(title)
                            .company(company)
                            .location(location)
                            .link(link != null ? (link.startsWith("http") ? link : "https://www.indeed.com" + link) : "")
                            .source("Indeed (Playwright-fallback)")
                            .build());
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return jobs;
    }

    @Override
    public String getName() {
        return "Playwright";
    }
}
