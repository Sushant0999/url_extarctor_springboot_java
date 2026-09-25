package com.url.extractor.service;

import com.url.extractor.dto.ExtractedData;
import com.url.extractor.helper.ExtractionStrategy;
import jakarta.inject.Singleton;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Singleton
public class ApiStrategy implements ExtractionStrategy {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public ExtractedData extract(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().isEmpty()) {
                return ExtractedData.builder().success(false).build();
            }

            return ExtractedData.builder()
                    .title("API Response")
                    .description("Content fetched via HttpClient")
                    .content(response.body())
                    .baseUrl(url)
                    .success(true)
                    .build();
        } catch (Exception e) {
            return ExtractedData.builder().success(false).build();
        }
    }

    @Override
    public String getName() {
        return "API";
    }
}
