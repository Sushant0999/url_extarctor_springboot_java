package com.url.extractor.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.url.extractor.dto.ExtractedData;
import com.url.extractor.helper.ExtractionStrategy;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class PlaywrightStrategy implements ExtractionStrategy {

    private Playwright playwright;
    private Browser browser;

    @PostConstruct
    public void init() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @Override
    public ExtractedData extract(String url) {
        try (BrowserContext context = browser.newContext();
             Page page = context.newPage()) {
            
            page.navigate(url);
            // Wait for the page to load and potentially for some JS to execute
            page.waitForLoadState(LoadState.NETWORKIDLE);

            String title = page.title();
            String content = page.innerText("body");
            
            // 📸 Capture Screenshot
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(false));
            String screenshotBase64 = java.util.Base64.getEncoder().encodeToString(screenshot);

            java.util.List<String> imageUrls = page.locator("img").all().stream()
                    .map(img -> img.getAttribute("src"))
                    .filter(src -> src != null && !src.isEmpty())
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            java.util.List<String> videoUrls = page.locator("video, video source").all().stream()
                    .map(v -> v.getAttribute("src"))
                    .filter(src -> src != null && !src.isEmpty())
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            java.util.List<String> anchorTags = page.locator("a").all().stream()
                    .map(a -> a.getAttribute("href"))
                    .filter(href -> href != null && !href.isEmpty() && (href.startsWith("http")))
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            ExtractedData data = ExtractedData.builder()
                    .title(title)
                    .topics(title)
                    .description("Rendered via Playwright")
                    .content(content)
                    .baseUrl(url)
                    .imageUrls(imageUrls)
                    .videoUrls(videoUrls)
                    .anchorTags(anchorTags)
                    .screenshotBase64(screenshotBase64)
                    .success(true)
                    .build();

            return data;
        } catch (Exception e) {
            return ExtractedData.builder().success(false).build();
        }
    }

    @Override
    public String getName() {
        return "Playwright";
    }

    @PreDestroy
    public void cleanup() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
