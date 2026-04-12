package com.url.extractor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.extractor.dto.JobDto;
import com.url.extractor.helper.JobExtractionStrategy;
import com.url.extractor.utils.MyLogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobJsoupStrategy implements JobExtractionStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<JobDto> extract(String url) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            MyLogger.info("JobJsoupStrategy: Fetching URL: " + url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(15000)
                    .get();

            // 1. Try Mosaic Data JSON
            Element script = doc.getElementById("mosaic-data");
            if (script != null) {
                jobs = parseMosaicData(script.data());
            }

            // 2. Fallback to CSS selectors if JSON not found/empty
            if (jobs.isEmpty()) {
                MyLogger.info("JobJsoupStrategy: JSON extraction failed, trying CSS selectors.");
                jobs = parseWithSelectors(doc);
            }

        } catch (IOException e) {
            MyLogger.err("JobJsoupStrategy: Failed to fetch/parse: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobDto> parseMosaicData(String json) {
        List<JobDto> jobs = new ArrayList<>();
        try {
            // Indeed sometimes wraps the JSON in window.mosaic.initialData = { ... };
            String jsonPart = json;
            if (json.contains("window.mosaic.initialData")) {
                int start = json.indexOf("{");
                int end = json.lastIndexOf("}");
                if (start != -1 && end != -1 && end > start) {
                    jsonPart = json.substring(start, end + 1);
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
                            .source("Indeed (Jsoup)")
                            .build());
                }
            }
        } catch (Exception e) {
            MyLogger.err("JobJsoupStrategy: Error parsing Mosaic JSON: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobDto> parseWithSelectors(Document doc) {
        List<JobDto> jobs = new ArrayList<>();
        doc.select(".job_seen_beacon").forEach(card -> {
            try {
                String title = card.select("h2.jobTitle span[title]").text();
                String company = card.select("span[data-testid='company-name']").text();
                String location = card.select("div[data-testid='text-location']").text();
                String link = card.select("a.jcs-JobTitle").attr("href");
                String salary = card.select(".salary-snippet-container, .estimated-salary").text();

                jobs.add(JobDto.builder()
                        .title(title)
                        .company(company)
                        .location(location)
                        .link(link.isEmpty() ? "" : (link.startsWith("http") ? link : "https://www.indeed.com" + link))
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
