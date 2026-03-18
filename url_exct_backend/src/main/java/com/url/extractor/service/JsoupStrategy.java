package com.url.extractor.service;

import com.url.extractor.dto.ExtractedData;
import com.url.extractor.helper.ExtractionStrategy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class JsoupStrategy implements ExtractionStrategy {

    @Override
    public ExtractedData extract(String url) {
        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();

            String title = document.title();
            String description = document.select("meta[name=description]").attr("content");
            String content = document.body().text();
            
            List<String> imageUrls = document.select("img").stream()
                    .map(img -> img.absUrl("src"))
                    .filter(src -> !src.isEmpty())
                    .distinct()
                    .toList();
            
            List<String> videoUrls = document.select("video, video source").stream()
                    .map(video -> video.absUrl("src"))
                    .filter(src -> !src.isEmpty())
                    .distinct()
                    .toList();

            List<String> anchorTags = document.select("a").stream()
                    .map(a -> a.absUrl("href"))
                    .filter(href -> !href.isEmpty() && (href.startsWith("http") || href.startsWith("https")))
                    .distinct()
                    .toList();

            return ExtractedData.builder()
                    .title(title)
                    .topics(title)
                    .description(description)
                    .content(content)
                    .baseUrl(url)
                    .imageUrls(imageUrls)
                    .videoUrls(videoUrls)
                    .anchorTags(anchorTags)
                    .success(true)
                    .build();
        } catch (IOException e) {
            return ExtractedData.builder().success(false).build();
        }
    }

    @Override
    public String getName() {
        return "Jsoup";
    }

    // Helper to get document for detection service
    public Document getDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(10000)
                .get();
    }
}
