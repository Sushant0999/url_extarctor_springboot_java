package com.url.extractor.controller;

import com.url.extractor.dto.ExtractedData;
import com.url.extractor.service.StrategySelector;
import com.url.extractor.service.MediaService;
import com.url.extractor.service.ExtractionStore;
import com.url.extractor.service.StorageService;
import com.url.extractor.service.ZipService;
import com.url.extractor.utils.MyLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/urlData")
@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600L, methods = {RequestMethod.GET, RequestMethod.OPTIONS, RequestMethod.POST})
public class UrlDataController {

    @Autowired
    private StrategySelector strategySelector;

    @Autowired
    private ExtractionStore extractionStore;

    @Autowired
    private StorageService storageService;

    @Autowired
    private ZipService zipService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private JsoupStrategy jsoupStrategy;

    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestBody List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return ResponseEntity.badRequest().body("No URLs provided");
        }
        
        String url = urls.get(0);
        MyLogger.info("Extraction requested for: " + url);
        try {
            ExtractedData extractedData = strategySelector.extract(url);
            if (extractedData == null || !extractedData.isSuccess()) {
                return ResponseEntity.status(500).body("Failed to extract data from URL");
            }

            // 🧠 Run Enhanced Analysis
            try {
                org.jsoup.nodes.Document doc = jsoupStrategy.getDocument(url);
                extractedData.setSeoIssues(analysisService.performSeoAudit(doc, extractedData));
                extractedData.setTechStack(analysisService.detectTechStack(doc));
                extractedData.setColorPalette(analysisService.extractColorPalette(doc));
                extractedData.setSummary(analysisService.generateSummary(extractedData));
            } catch (Exception e) {
                MyLogger.warn("Enhanced analysis failed: " + e.getMessage());
            }

            String storagePath = storageService.saveExtractedData(extractedData);
            extractionStore.save(extractedData, storagePath);
            return ResponseEntity.ok(extractedData);
        } catch (Exception e) {
            MyLogger.err("Error during extraction: " + e.getMessage());
            return ResponseEntity.internalServerError().body("SOMETHING WENT WRONG: " + e.getMessage());
        }
    }

    @Autowired
    private MediaService mediaService;

    @GetMapping("/media")
    public ResponseEntity<?> getMedia() {
        ExtractedData lastData = extractionStore.getLastResult();
        
        if (lastData == null) {
            return ResponseEntity.badRequest().body("No extracted data available. Please run /extract first.");
        }
        
        List<String> imageUrls = lastData.getImageUrls();
        List<String> videoUrls = lastData.getVideoUrls();
        
        if ((imageUrls == null || imageUrls.isEmpty()) && (videoUrls == null || videoUrls.isEmpty())) {
            return ResponseEntity.badRequest().body("No media links found in the last extraction.");
        }
        
        MyLogger.info("Media content download requested for " + 
                (imageUrls != null ? imageUrls.size() : 0) + " images and " + 
                (videoUrls != null ? videoUrls.size() : 0) + " videos");
                
        try {
            List<byte[]> media = mediaService.downloadMedia(imageUrls, videoUrls);
            if (media.isEmpty()) {
                return ResponseEntity.status(404).body("No media content could be downloaded from stored links");
            }
            return ResponseEntity.ok(media);
        } catch (Exception e) {
            MyLogger.err("Error during media download: " + e.getMessage());
            return ResponseEntity.internalServerError().body("FAILED TO FETCH MEDIA CONTENT: " + e.getMessage());
        }
    }

    @GetMapping("/getTags")
    public ResponseEntity<List<String>> getTags() {
        ExtractedData lastData = extractionStore.getLastResult();
        if (lastData == null || lastData.getAnchorTags() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(lastData.getAnchorTags());
    }

    @GetMapping("/getText")
    public ResponseEntity<List<String>> getText() {
        ExtractedData lastData = extractionStore.getLastResult();
        if (lastData == null || lastData.getContent() == null || lastData.getContent().isEmpty()) {
            return ResponseEntity.ok(List.of("No data available for the last extraction."));
        }

        // Properly split content into paragraphs and filter empty/short fragments
        List<String> paragraphs = Arrays.stream(lastData.getContent().split("\\n+"))
                .map(String::trim)
                .filter(s -> s.length() > 5)
                .distinct()
                .limit(100)
                .collect(Collectors.toList());

        return ResponseEntity.ok(paragraphs.isEmpty() ? List.of("No significant text content detected.") : paragraphs);
    }

    @PostMapping("/insert")
    public ResponseEntity<?> insertBulk(@RequestBody List<String> urls, @RequestParam(value = "jsEnable", required = false) Boolean jsEnable) {
        return extract(urls);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadZip() {
        String lastStoragePath = extractionStore.getLastStoragePath();
        if (lastStoragePath == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path zipPath = zipService.zipDirectory(lastStoragePath);
            Resource resource = new UrlResource(zipPath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipPath.getFileName().toString() + "\"")
                    .body(resource);
        } catch (Exception e) {
            MyLogger.err("Error creating zip: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
