package com.url.extractor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.extractor.dto.JobDto;
import com.url.extractor.helper.JobExtractionStrategy;
import com.url.extractor.utils.MyLogger;
import jakarta.inject.Singleton;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class JobJsoupStrategy implements JobExtractionStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<JobDto> extract(String url) {
        if (!url.toLowerCase().contains("indeed.com")) {
            // Early bypass: Jsoup strategy is currently only coded for Indeed DOM structures
            return new ArrayList<>();
        }
        
        List<JobDto> jobs = new ArrayList<>();
        try {
            MyLogger.info("JobJsoupStrategy: Fetching URL: " + url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Sec-Ch-Ua", "\"Chromium\";v=\"130\", \"Google Chrome\";v=\"130\", \"Not?A_Brand\";v=\"99\"")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Referer", "https://www.google.com/")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(20000)
                    .get();

            // 1. Try script tags with Mosaic or embedded job JSON
            for (Element script : doc.select("script")) {
                String data = script.data();
                if (data.contains("mosaic-provider-jobcards") || data.contains("window.mosaic") || (data.contains("\"jobkey\"") && data.contains("\"title\""))) {
                    jobs = parseMosaicData(data);
                    if (!jobs.isEmpty()) {
                        MyLogger.info("JobJsoupStrategy: Extracted " + jobs.size() + " jobs from embedded script JSON");
                        return jobs;
                    }
                }
            }

            // 2. Try JSON-LD structured data (Schema.org JobPosting)
            for (Element script : doc.select("script[type='application/ld+json']")) {
                jobs = parseJsonLd(script.data());
                if (!jobs.isEmpty()) {
                    MyLogger.info("JobJsoupStrategy: Extracted " + jobs.size() + " jobs from JSON-LD");
                    return jobs;
                }
            }

            // 3. Fallback to broad CSS selectors
            MyLogger.info("JobJsoupStrategy: JSON extraction empty, trying CSS selectors...");
            jobs = parseWithSelectors(doc);
            if (!jobs.isEmpty()) {
                MyLogger.info("JobJsoupStrategy: Extracted " + jobs.size() + " jobs via CSS selectors");
            }

        } catch (IOException e) {
            MyLogger.err("JobJsoupStrategy: Failed to fetch/parse: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobDto> parseMosaicData(String json) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            String jsonPart = json;
            int start = jsonPart.indexOf('{');
            int end = jsonPart.lastIndexOf('}');
            if (start != -1 && end > start) {
                jsonPart = jsonPart.substring(start, end + 1);
            }

            JsonNode root = objectMapper.readTree(jsonPart);
            JsonNode results = root.findValue("results");

            if (results != null && results.isArray()) {
                for (JsonNode node : results) {
                    String title = node.path("title").asText();
                    if (title.isEmpty()) title = node.path("jobTitle").asText();
                    if (title.isEmpty()) continue;

                    String company = node.path("company").asText();
                    String loc = node.path("formattedLocation").asText();
                    if (loc.isEmpty()) loc = node.path("location").asText();

                    String jobKey = node.path("jobkey").asText();
                    String link = !jobKey.isEmpty() ? "https://in.indeed.com/viewjob?jk=" + jobKey : "";

                    String salary = node.path("salaryText").path("text").asText("Not disclosed");
                    String date = node.path("formattedRelativeTime").asText();

                    jobs.add(JobDto.builder()
                            .title(title)
                            .company(company.isEmpty() ? "Company Confidential" : company)
                            .location(loc.isEmpty() ? "India" : loc)
                            .link(link)
                            .salary(salary)
                            .datePosted(date)
                            .source("Indeed (Jsoup)")
                            .build());
                }
            }
        } catch (Exception e) {
            MyLogger.err("JobJsoupStrategy: Error parsing Mosaic JSON: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobDto> parseJsonLd(String json) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.isArray()) {
                for (JsonNode item : root) {
                    addJobIfPosting(item, jobs);
                }
            } else {
                addJobIfPosting(root, jobs);
                JsonNode items = root.path("itemListElement");
                if (items.isArray()) {
                    for (JsonNode elem : items) {
                        JsonNode item = elem.has("item") ? elem.path("item") : elem;
                        addJobIfPosting(item, jobs);
                    }
                }
            }
        } catch (Exception ignored) {}
        return jobs;
    }

    private void addJobIfPosting(JsonNode node, List<JobDto> jobs) {
        String type = node.path("@type").asText();
        if ("JobPosting".equalsIgnoreCase(type)) {
            String title = node.path("title").asText();
            String company = node.path("hiringOrganization").path("name").asText("Company Confidential");
            String loc = node.path("jobLocation").path("address").path("addressLocality").asText("India");
            String link = node.path("url").asText();

            if (!title.isEmpty()) {
                jobs.add(JobDto.builder()
                        .title(title)
                        .company(company)
                        .location(loc)
                        .link(link)
                        .salary("Not disclosed")
                        .source("Indeed (Jsoup-JSONLD)")
                        .build());
            }
        }
    }

    private List<JobDto> parseWithSelectors(Document doc) {
        List<JobDto> jobs = new ArrayList<>();
        doc.select(".job_seen_beacon, div.cardOutline, li:has(a.jcs-JobTitle), div[data-testid='slider_item'], td.resultContent").forEach(card -> {
            try {
                String title = card.select("h2.jobTitle span[title], a.jcs-JobTitle span, h2.jobTitle a, [data-testid='jobTitle'], h2.jobTitle").text();
                String company = card.select("span[data-testid='company-name'], span.companyName, span.css-63koeb, [data-testid='company-name']").text();
                String location = card.select("div[data-testid='text-location'], div.companyLocation, [data-testid='text-location']").text();
                String link = card.select("a.jcs-JobTitle, h2.jobTitle a, a[data-jk]").attr("href");
                String salary = card.select("[data-testid='attribute_snippet_testid'], .salary-snippet-container, .estimated-salary").text();

                if (title.isEmpty()) return;

                if (link.isEmpty()) {
                    String jk = card.attr("data-jk");
                    if (!jk.isEmpty()) {
                        link = "https://in.indeed.com/viewjob?jk=" + jk;
                    }
                } else if (!link.startsWith("http")) {
                    link = "https://in.indeed.com" + link;
                }

                jobs.add(JobDto.builder()
                        .title(title)
                        .company(company.isEmpty() ? "Company Confidential" : company)
                        .location(location.isEmpty() ? "India" : location)
                        .link(link)
                        .salary(salary.isEmpty() ? "Not disclosed" : salary)
                        .source("Indeed (Jsoup-fallback)")
                        .build());
            } catch (Exception ignored) {}
        });
        return jobs;
    }

    @Override
    public String getName() {
        return "Jsoup";
    }
}
