package com.url.extractor.service;

import com.url.extractor.dto.ExtractedData;
import com.url.extractor.helper.ExtractionStrategy;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ApiStrategy implements ExtractionStrategy {

    private final WebClient webClient;

    public ApiStrategy(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public ExtractedData extract(String url) {
        try {
            String responseBody = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(e -> Mono.empty())
                    .block();

            if (responseBody == null || responseBody.isEmpty()) {
                return ExtractedData.builder().success(false).build();
            }

            // Simplistic extraction for API strategy - usually more complex logic would go here
            // to parse JSON if detected, but for now we treat it as raw content.
            return ExtractedData.builder()
                    .title("API Response")
                    .description("Content fetched via WebClient")
                    .content(responseBody)
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
