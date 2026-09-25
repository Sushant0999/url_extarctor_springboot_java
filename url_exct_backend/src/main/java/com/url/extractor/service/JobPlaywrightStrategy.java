package com.url.extractor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.url.extractor.dto.JobDto;
import com.url.extractor.helper.JobExtractionStrategy;
import com.url.extractor.utils.MyLogger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Singleton
public class JobPlaywrightStrategy implements JobExtractionStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Inject
    @Named("playwrightSemaphore")
    private Semaphore playwrightSemaphore;

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
                 Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setChannel("chrome") // Enforce real browser TLS signatures
                    .setArgs(List.of("--disable-blink-features=AutomationControlled"))
                 );
                 BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                 );
                 Page page = context.newPage()) {

                // Bypass basic bot detection (e.g. Akamai, Cloudflare) usually affecting Naukri & LinkedIn
                page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

                page.setExtraHTTPHeaders(Map.of(
                    "Accept-Language", "en-US,en;q=0.9",
                    "Referer", "https://www.google.com/"
                ));

                page.navigate(url, new Page.NavigateOptions().setTimeout(60000).setWaitUntil(com.microsoft.playwright.options.WaitUntilState.LOAD));
                
                // Extra delay to simulate human focus
                randomDelay(1000, 2000);
                
                // Auto-scroll to trigger lazy loading (crucial for LinkedIn & Indeed)
                autoScroll(page);
                randomDelay(1000, 2000);

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
                } else if (url.contains("hirist.tech") || url.contains("hirist.com")) {
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
            // Naukri 2025: uses article[data-job-id] or [class*='srp-jobtuple-wrapper'] or [class*='jobTuple']
            Locator cards = page.locator(
                "article[data-job-id], " +
                "[class*='srp-jobtuple-wrapper'], " +
                "article.jobTuple, " +
                ".cust-job-tuple, " +
                "[class*='jobTuple']"
            );
            try { cards.first().waitFor(new Locator.WaitForOptions().setTimeout(12000)); } catch (Exception ignored) {}

            int count = cards.count();
            if (count == 0) {
                try {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("debug_naukri.html"), page.innerHTML("body"));
                    MyLogger.warn("JobPlaywrightStrategy: Dumped Naukri HTML to debug_naukri.html for selector debugging.");
                } catch (Exception e) {}
            }
            MyLogger.info("JobPlaywrightStrategy: Found " + count + " Naukri job cards.");

            int success = 0;
            for (int i = 0; i < count; i++) {
                Locator card = cards.nth(i);
                try {
                    // 2025 selectors: data attributes preferred, fall back to class names
                    String title = safeText(card, 
                        "a.title, [class*='title'], .jobTitle, a[class*='jobtitle'], h2 a, h3 a");
                    if (title.isEmpty()) continue;

                    String company = safeText(card,
                        "a.subTitle, [class*='comp-name'], [class*='companyInfo'], " +
                        "[class*='company-name'], .company-name, [class*='company']");
                    String location = safeText(card,
                        "[class*='locWrp'], [class*='locWraper'], [class*='loc-wrap'], " +
                        ".loc-wrap, [class*='location'], [class*='naukri__location']");
                    String salary = safeText(card,
                        "[class*='salary'], [class*='sal'], [class*='compensation']");
                    String date = safeText(card,
                        ".job-post-day, [class*='posted'], [class*='freshness']");

                    // Link: prefer data-job-id anchor
                    String link = null;
                    try { link = card.locator("a.title, a[class*='jobtitle'], a[class*='title']").first().getAttribute("href"); } catch (Exception ignored) {}
                    if (link == null) { try { link = card.locator("a").first().getAttribute("href"); } catch (Exception ig) {} }
                    if (link != null && !link.startsWith("http")) link = "https://www.naukri.com" + link;

                    jobs.add(JobDto.builder()
                            .title(title)
                            .company(company.isEmpty() ? "Unknown" : company)
                            .location(location.isEmpty() ? null : location)
                            .salary(salary.isEmpty() ? null : salary)
                            .link(link)
                            .datePosted(date.isEmpty() ? null : date)
                            .source("Naukri")
                            .build());
                    success++;
                } catch (Exception e) {
                    MyLogger.warn("JobPlaywrightStrategy: Naukri card " + i + " failed: " + e.getMessage());
                }
            }
            MyLogger.info("JobPlaywrightStrategy: Naukri extracted " + success + "/" + count + " jobs.");
        } catch (Exception e) {
            MyLogger.err("JobPlaywrightStrategy: Naukri extraction failed: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobDto> extractFromCutshort(Page page) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            // Cutshort recently obfuscated their entire DOM with styled-components hashes.
            // Using a structural XPath to identify the closest bounding box around an anchor link to /job/ and /company/
            Locator cards = page.locator("xpath=//a[contains(@href, '/job/')]/ancestor::div[a[contains(@href, '/company/')]][1]");
            try { cards.first().waitFor(new Locator.WaitForOptions().setTimeout(10000)); } catch (Exception ignored) {}
            
            int count = cards.count();
            if (count == 0) {
                try {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("debug_cutshort.html"), page.innerHTML("body"));
                } catch (Exception e) {}
            }
            MyLogger.info("JobPlaywrightStrategy: Found " + count + " Cutshort job cards.");

            for (int i = 0; i < count; i++) {
                Locator card = cards.nth(i);
                try {
                    // Title is inside the inner a[href*='/job/'] element
                    String title = card.locator("a[href*='/job/']").first().innerText().trim();
                    // Company is generally prefixed with "at " or inside the company link
                    String company = card.locator("a[href*='/company/']").first().innerText().trim();
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
            try { cards.first().waitFor(new Locator.WaitForOptions().setTimeout(10000)); } catch (Exception ignored) {}
            
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
            try { cards.first().waitFor(new Locator.WaitForOptions().setTimeout(10000)); } catch (Exception ignored) {}
            
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
            // Shine is Next.js. Try __NEXT_DATA__ JSON extraction first — most reliable.
            try {
                String nextData = (String) page.evaluate(
                    "() => { const el = document.getElementById('__NEXT_DATA__'); return el ? el.textContent : null; }"
                );
                if (nextData != null && !nextData.isBlank()) {
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(nextData);
                    // Shine stores job results under pageProps.jobResults.hits or similar
                    com.fasterxml.jackson.databind.JsonNode hits = root.at("/props/pageProps/jobResults/hits");
                    if (hits == null || hits.isMissingNode()) hits = root.findValue("hits");
                    if (hits != null && hits.isArray() && hits.size() > 0) {
                        MyLogger.info("JobPlaywrightStrategy: Shine __NEXT_DATA__ found " + hits.size() + " hits.");
                        for (com.fasterxml.jackson.databind.JsonNode hit : hits) {
                            String title = hit.path("designation").asText("");
                            if (title.isEmpty()) title = hit.path("jobTitle").asText("");
                            if (title.isEmpty()) title = hit.path("title").asText("");
                            if (title.isEmpty()) continue;
                            String company = hit.path("company").path("name").asText("");
                            if (company.isEmpty()) company = hit.path("companyName").asText("Unknown");
                            String location = hit.path("location").asText("");
                            if (location.isEmpty()) location = hit.path("city").asText("");
                            String salary = hit.path("salary").asText("");
                            String jobId = hit.path("jobId").asText("");
                            String slug = hit.path("slug").asText("");
                            String link = slug.isEmpty() ? (jobId.isEmpty() ? null : "https://www.shine.com/jobs/" + jobId)
                                                        : "https://www.shine.com/jobs/" + slug;
                            jobs.add(JobDto.builder()
                                    .title(title).company(company)
                                    .location(location.isEmpty() ? null : location)
                                    .salary(salary.isEmpty() ? null : salary)
                                    .link(link).source("Shine").build());
                        }
                        return jobs;
                    }
                }
            } catch (Exception e) {
                MyLogger.warn("JobPlaywrightStrategy: Shine __NEXT_DATA__ extraction failed: " + e.getMessage());
            }

            // DOM fallback: Confirmed live selectors (verified 2025-09-25)
            // Card: <article>, Link: <a class='result-card_hit__HASH' aria-label='Title at Company'>
            Locator cards = page.locator("article");
            try { cards.first().waitFor(new Locator.WaitForOptions().setTimeout(15000)); } catch (Exception ignored) {}

            int count = cards.count();
            if (count == 0) {
                // Fallback: try older class-based selectors in case Shine rolled back
                cards = page.locator(".jobCard, [class*='jobCard']:not([class*='jobCardSkeleton']), [class*='result-card']");
                try { cards.first().waitFor(new Locator.WaitForOptions().setTimeout(5000)); } catch (Exception ignored) {}
                count = cards.count();
            }
            MyLogger.info("JobPlaywrightStrategy: Found " + count + " Shine job cards.");

            if (count == 0) {
                try {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("debug_shine.html"), page.innerHTML("body"));
                    MyLogger.warn("JobPlaywrightStrategy: Dumped Shine HTML to debug_shine.html");
                } catch (Exception ignored) {}
            }

            int success = 0;
            for (int i = 0; i < count; i++) {
                Locator card = cards.nth(i);
                try {
                    // Primary: parse from the anchor's aria-label: "Job Title at Company Name"
                    String title = "";
                    String company = "Unknown";
                    String link = null;

                    // Try to find the primary anchor with aria-label
                    Locator anchor = card.locator("a[aria-label], a[class*='result-card_hit'], a[class*='hit'], a[href*='/jobs/']").first();
                    if (anchor.count() > 0) {
                        String ariaLabel = anchor.getAttribute("aria-label");
                        link = anchor.getAttribute("href");

                        if (ariaLabel != null && ariaLabel.contains(" at ")) {
                            int atIdx = ariaLabel.lastIndexOf(" at ");
                            title = ariaLabel.substring(0, atIdx).trim();
                            company = ariaLabel.substring(atIdx + 4).trim();
                        } else {
                            // Fallback: read h2/h3 as title
                            title = safeText(card, "h2, h3, [class*='title']");
                            company = safeText(card, "[class*='company'], [class*='Company']");
                        }
                    }

                    // Second fallback: get title from h2/h3 if still empty
                    if (title.isEmpty()) {
                        title = safeText(card, "h2, h3, [class*='jobTitle'], [class*='title']");
                    }
                    if (title.isEmpty()) continue; // skip skeleton/empty cards

                    // Links
                    if (link == null) {
                        try { link = card.locator("a").first().getAttribute("href"); } catch (Exception ignored) {}
                    }
                    if (link != null && !link.startsWith("http")) link = "https://www.shine.com" + link;

                    // Location & salary - use broad class fragment matching
                    String location = safeText(card, "[class*='location'], [class*='Location'], [class*='loc'], [class*='city']");
                    String salary = safeText(card, "[class*='salary'], [class*='Salary'], [class*='ctc'], [class*='lakh']");
                    String experience = safeText(card, "[class*='exp'], [class*='experience']");

                    jobs.add(JobDto.builder()
                            .title(title)
                            .company(company.isEmpty() ? "Unknown" : company)
                            .location(location.isEmpty() ? null : location)
                            .salary(salary.isEmpty() ? (experience.isEmpty() ? null : experience) : salary)
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
            try { cards.first().waitFor(new Locator.WaitForOptions().setTimeout(10000)); } catch (Exception ignored) {}
            
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
            try { cards.first().waitFor(new Locator.WaitForOptions().setTimeout(10000)); } catch (Exception ignored) {}
            
            int count = cards.count();
            if (count == 0) {
                try {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("debug_linkedin.html"), page.innerHTML("body"));
                } catch (Exception e) {}
            }
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
            try { cards.first().waitFor(new Locator.WaitForOptions().setTimeout(10000)); } catch (Exception ignored) {}
            
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

    private void autoScroll(Page page) {
        try {
            page.evaluate("async () => {" +
                "  await new Promise((resolve) => {" +
                "    let totalHeight = 0;" +
                "    let distance = 100;" +
                "    let timer = setInterval(() => {" +
                "      let scrollHeight = document.body.scrollHeight;" +
                "      window.scrollBy(0, distance);" +
                "      totalHeight += distance;" +
                "      if(totalHeight >= scrollHeight || totalHeight > 3000){" +
                "        clearInterval(timer);" +
                "        resolve();" +
                "      }" +
                "    }, 100);" +
                "  });" +
                "}");
        } catch (Exception e) {
            MyLogger.warn("JobPlaywrightStrategy: Auto-scroll failed: " + e.getMessage());
        }
    }

    private void randomDelay(int min, int max) {
        try {
            int delay = min + (int) (Math.random() * (max - min));
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getName() {
        return "Playwright";
    }
}
