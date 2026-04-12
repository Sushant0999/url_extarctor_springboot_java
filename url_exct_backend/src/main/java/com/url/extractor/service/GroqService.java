package com.url.extractor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.extractor.utils.MyLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
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
                           "1. jobTitle (Extract a clean job title. If terms like 'Fresher', 'Junior', 'Intern', 'Student' are present, include them or infer them) " +
                           "2. skills (List of technical or professional skills) " +
                           "3. experienceLevel (One of: entry_level, mid_level, senior_level. Map 'Fresher', 'Entry', 'Junior' to 'entry_level') " +
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
}
