package com.url.extractor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.extractor.utils.MyLogger;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class GroqService {

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String parseResume(String text) {
        try {
            MyLogger.info("GroqService: Sending resume text to Groq API...");
            
            // Limit text size to avoid token limit (typical for Groq)
            String truncatedText = text.length() > 10000 ? text.substring(0, 10000) : text;

            Map<String, Object> body = new HashMap<>();
            body.put("model", "llama-3.3-70b-versatile");
            
            var systemMessage = Map.of(
                "role", "system",
                "content", "You are a professional resume parser. Extract the following from the resume text: " +
                           "1. jobTitle (Extract a clean job title strictly based on their MOST RECENT or CURRENT experience. If terms like 'Fresher', 'Junior', 'Intern', 'Student' are present, include them or infer them) " +
                           "2. skills (List of technical or professional skills) " +
                           "3. experienceLevel (One of: entry_level, mid_level, senior_level. If total experience is 1-3 years or titles contain 'Junior' or 'Associate', return 'entry_level'. If total experience is 3-5 years or titles contain 'Intermediate', return 'mid_level'. If experience is 5+ years or titles contain 'Senior', return 'senior_level') " +
                           "4. location (Extract the State or Province if mentioned) " +
                           "Return ONLY a clean JSON object. Example: {\"jobTitle\": \"Java Developer (Fresher)\", \"skills\": [\"Spring\", \"React\"], \"experienceLevel\": \"entry_level\", \"location\": \"Maharashtra\"}"
            );

            var userMessage = Map.of(
                "role", "user",
                "content", "Parse this resume: " + truncatedText
            );

            body.put("messages", java.util.Arrays.asList(systemMessage, userMessage));
            body.put("response_format", Map.of("type", "json_object"));

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String content = root.path("choices").path(0).path("message").path("content").asText();
                MyLogger.info("GroqService: Successfully parsed resume.");
                return content;
            } else {
                MyLogger.err("GroqService: API Error! Status: " + response.statusCode() + " | Body: " + response.body());
                return null;
            }
        } catch (Exception e) {
            MyLogger.err("GroqService: Error parsing resume: " + e.getMessage());
            return null;
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public String generatePlatformSearchUrl(String platform, com.url.extractor.dto.JobSearchFilter filter, String fallbackUrl) {
        if (!hasApiKey()) {
            return fallbackUrl;
        }

        try {
            MyLogger.info("GroqService: Asking Groq to build/verify latest actual URL for platform: " + platform);

            String role = filter.getQuery() != null && !filter.getQuery().isBlank() ? filter.getQuery() :
                          (filter.getSkills() != null && !filter.getSkills().isEmpty() ? String.join(" ", filter.getSkills()) : "Software Developer");
            String location = (filter.getLocations() != null && !filter.getLocations().isEmpty()) ? String.join(", ", filter.getLocations()) : "";
            String country = filter.getCountry() != null ? filter.getCountry() : "in";

            Map<String, Object> body = new HashMap<>();
            body.put("model", "llama-3.3-70b-versatile");
            body.put("temperature", 0.1);

            var systemMessage = Map.of(
                "role", "system",
                "content", "You are an expert web scraping URL builder. Given a job platform, target job role, location, and country, generate the EXACT modern search URL used by that platform in 2025/2026.\n" +
                           "Known conventions:\n" +
                           "- indeed (India): https://in.indeed.com/q-{role-slug}-jobs.html (or https://in.indeed.com/jobs?q=...)\n" +
                           "- linkedin (India): https://in.linkedin.com/jobs/{role-slug}-jobs?position=1&pageNum=0\n" +
                           "- shine: https://www.shine.com/job-search/{role-slug}-jobs\n" +
                           "- naukri: https://www.naukri.com/{role-slug}-jobs\n" +
                           "- internshala: https://internshala.com/jobs/{role-slug}-jobs\n" +
                           "- hirist: https://www.hirist.tech/search?q={role}\n" +
                           "- foundit: https://www.foundit.in/srp/results?query={role}\n" +
                           "Return ONLY a clean JSON object: {\"url\": \"https://...\"}. If uncertain, return: {\"url\": \"" + fallbackUrl + "\"}"
            );

            var userMessage = Map.of(
                "role", "user",
                "content", "Platform: " + platform + "\nRole: " + role + "\nLocation: " + location + "\nCountry: " + country + "\nFallbackUrl: " + fallbackUrl
            );

            body.put("messages", java.util.Arrays.asList(systemMessage, userMessage));
            body.put("response_format", Map.of("type", "json_object"));

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String content = root.path("choices").path(0).path("message").path("content").asText();
                JsonNode parsed = objectMapper.readTree(content);
                String generatedUrl = parsed.path("url").asText();
                if (generatedUrl != null && (generatedUrl.startsWith("http://") || generatedUrl.startsWith("https://"))) {
                    MyLogger.info("GroqService: Successfully generated URL for " + platform + ": " + generatedUrl);
                    return generatedUrl;
                }
            } else {
                MyLogger.err("GroqService: URL generation API returned status " + response.statusCode());
            }
        } catch (Exception e) {
            MyLogger.err("GroqService: Error generating URL via Groq: " + e.getMessage());
        }

        return fallbackUrl;
    }
}
