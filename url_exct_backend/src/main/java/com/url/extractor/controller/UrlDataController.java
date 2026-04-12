package com.url.extractor.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.url.extractor.dto.ExtractedData;
import com.url.extractor.service.*;
import com.url.extractor.utils.MyLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/urlData")
@CrossOrigin(origins = "${app.allowed.origins}", allowedHeaders = "*", maxAge = 3600L, methods = { RequestMethod.GET, RequestMethod.OPTIONS,
        RequestMethod.POST })
@Tag(name = "URL Extraction API", description = "Endpoints for background URL data extraction and status tracking. Supports multiple URLs in a single request.")
public class UrlDataController {

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private ExtractionStore extractionStore;

    @Autowired
    private TaskTrackerService taskTrackerService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private ZipService zipService;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("UrlDataController is active!");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // EXTRACT
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Accepts a single URL string OR an array of URLs.
     * Returns a map of { url -> taskId } for tracking each URL independently.
     *
     * Example single:   "https://example.com"
     * Example multiple: ["https://a.com", "https://b.com"]
     */
    @PostMapping("/extract")
    @Operation(
        summary = "Start URL extraction tasks",
        description = "Accepts a single URL string or a JSON array of URLs. Returns a map of { url -> taskId } for each submitted URL."
    )
    public ResponseEntity<Map<String, String>> handleExtraction(@RequestBody JsonNode input) {
        List<String> urls = new ArrayList<>();

        if (input.isArray()) {
            for (JsonNode node : input) {
                String text = node.asText().trim();
                if (!text.isEmpty()) {
                    urls.add(text);
                }
            }
        } else if (input.isTextual()) {
            String text = input.asText().trim();
            if (!text.isEmpty()) {
                urls.add(text);
            }
        } else {
            return ResponseEntity.badRequest().build();
        }

        if (urls.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, String> taskIdMap = extractionService.processBulk(urls);
        return ResponseEntity.ok(taskIdMap);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // STATUS & RESULT (per task)
    // ─────────────────────────────────────────────────────────────────────────────

    @GetMapping("/status/{taskId}")
    @Operation(summary = "Get task status", description = "Returns the current state of a task: PENDING, IN_PROGRESS, COMPLETED, or FAILED.")
    public ResponseEntity<String> getStatus(
            @Parameter(description = "Task ID returned from /extract") @PathVariable String taskId) {
        var status = taskTrackerService.getStatus(taskId);
        return ResponseEntity.ok(status != null ? status.name() : "NOT_FOUND");
    }

    @GetMapping("/result/{taskId}")
    @Operation(summary = "Get extracted result for a single task", description = "Retrieves the full ExtractedData object for a completed task.")
    public ResponseEntity<ExtractedData> getResult(
            @Parameter(description = "Task ID returned from /extract") @PathVariable String taskId) {
        ExtractedData result = taskTrackerService.getResult(taskId);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    /**
     * Bulk status check: POST a JSON array of taskIds → get back { taskId -> status } map.
     */
    @PostMapping("/status/bulk")
    @Operation(
        summary = "Bulk status check",
        description = "POST a JSON array of taskIds and receive a map of { taskId -> status } for all of them."
    )
    public ResponseEntity<Map<String, String>> getBulkStatus(@RequestBody List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, String> statusMap = new LinkedHashMap<>();
        for (String taskId : taskIds) {
            var status = taskTrackerService.getStatus(taskId);
            statusMap.put(taskId, status != null ? status.name() : "NOT_FOUND");
        }
        return ResponseEntity.ok(statusMap);
    }

    /**
     * Bulk results: POST a JSON array of taskIds → get back { taskId -> ExtractedData } map.
     * Only tasks that are COMPLETED and have results are included.
     */
    @PostMapping("/results/bulk")
    @Operation(
        summary = "Bulk results retrieval",
        description = "POST a JSON array of taskIds and receive a map of { taskId -> ExtractedData } for each completed task."
    )
    public ResponseEntity<Map<String, ExtractedData>> getBulkResults(@RequestBody List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, ExtractedData> resultsMap = new LinkedHashMap<>();
        for (String taskId : taskIds) {
            ExtractedData data = taskTrackerService.getResult(taskId);
            if (data != null) {
                resultsMap.put(taskId, data);
            }
        }
        return ResponseEntity.ok(resultsMap);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SSE SUBSCRIPTION (per task)
    // ─────────────────────────────────────────────────────────────────────────────

    @GetMapping(value = "/subscribe/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to task updates (SSE)", description = "Real-time Server-Sent Events stream for a specific task.")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter subscribe(
            @Parameter(description = "Task ID to subscribe to") @PathVariable String taskId) {
        return taskTrackerService.subscribe(taskId);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // MEDIA  (per task)
    // ─────────────────────────────────────────────────────────────────────────────

    @GetMapping("/media/{taskId}")
    @Operation(
        summary = "Download media for a specific task",
        description = "Downloads all images and videos found during extraction for the given taskId."
    )
    public ResponseEntity<?> getMedia(
            @Parameter(description = "Task ID returned from /extract") @PathVariable String taskId) {

        ExtractedData data = extractionStore.getResult(taskId);
        if (data == null) {
            return ResponseEntity.badRequest()
                    .body("No extracted data available for taskId: " + taskId + ". Please run extraction first.");
        }

        List<String> imageUrls = data.getImageUrls();
        List<String> videoUrls = data.getVideoUrls();

        if ((imageUrls == null || imageUrls.isEmpty()) && (videoUrls == null || videoUrls.isEmpty())) {
            return ResponseEntity.badRequest().body("No media links found for taskId: " + taskId);
        }

        try {
            List<byte[]> media = mediaService.downloadMedia(imageUrls, videoUrls);
            if (media.isEmpty()) {
                return ResponseEntity.status(404).body("No media content could be downloaded for taskId: " + taskId);
            }
            return ResponseEntity.ok(media);
        } catch (Exception e) {
            MyLogger.err("Error during media download for taskId " + taskId + ": " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("FAILED TO FETCH MEDIA CONTENT: " + e.getMessage());
        }
    }

    /**
     * Bulk media info: POST a list of taskIds → get back { taskId -> { imageUrls, videoUrls } }.
     * Does not download the actual bytes — returns the URL lists for each task.
     */
    @PostMapping("/media/bulk")
    @Operation(
        summary = "Bulk media URL retrieval",
        description = "POST a JSON array of taskIds and get back a map of { taskId -> { imageUrls, videoUrls } }."
    )
    public ResponseEntity<Map<String, Map<String, List<String>>>> getBulkMedia(@RequestBody List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, Map<String, List<String>>> response = new LinkedHashMap<>();
        for (String taskId : taskIds) {
            ExtractedData data = extractionStore.getResult(taskId);
            if (data != null) {
                Map<String, List<String>> mediaMap = new LinkedHashMap<>();
                mediaMap.put("imageUrls", data.getImageUrls() != null ? data.getImageUrls() : List.of());
                mediaMap.put("videoUrls", data.getVideoUrls() != null ? data.getVideoUrls() : List.of());
                response.put(taskId, mediaMap);
            }
        }
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TAGS  (per task)
    // ─────────────────────────────────────────────────────────────────────────────

    @GetMapping("/getTags/{taskId}")
    @Operation(summary = "Get anchor tags for a specific task", description = "Returns all anchor/href tags extracted from the page for the given taskId.")
    public ResponseEntity<List<String>> getTags(
            @Parameter(description = "Task ID returned from /extract") @PathVariable String taskId) {
        ExtractedData data = extractionStore.getResult(taskId);
        if (data == null || data.getAnchorTags() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(data.getAnchorTags());
    }

    /**
     * Bulk tags: POST a list of taskIds → get back { taskId -> [anchorTags] }.
     */
    @PostMapping("/getTags/bulk")
    @Operation(
        summary = "Bulk anchor tags retrieval",
        description = "POST a JSON array of taskIds and receive a map of { taskId -> [anchorTags] }."
    )
    public ResponseEntity<Map<String, List<String>>> getBulkTags(@RequestBody List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, List<String>> response = new LinkedHashMap<>();
        for (String taskId : taskIds) {
            ExtractedData data = extractionStore.getResult(taskId);
            response.put(taskId, (data != null && data.getAnchorTags() != null) ? data.getAnchorTags() : List.of());
        }
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEXT  (per task)
    // ─────────────────────────────────────────────────────────────────────────────

    @GetMapping("/getText/{taskId}")
    @Operation(summary = "Get page text for a specific task", description = "Returns meaningful text paragraphs extracted from the page for the given taskId.")
    public ResponseEntity<List<String>> getText(
            @Parameter(description = "Task ID returned from /extract") @PathVariable String taskId) {
        ExtractedData data = extractionStore.getResult(taskId);
        if (data == null || data.getContent() == null || data.getContent().isEmpty()) {
            return ResponseEntity.ok(List.of("No data available for taskId: " + taskId));
        }

        List<String> paragraphs = Arrays.stream(data.getContent().split("\\n+"))
                .map(String::trim)
                .filter(s -> s.length() > 5)
                .distinct()
                .limit(100)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                paragraphs.isEmpty() ? List.of("No significant text content detected.") : paragraphs);
    }

    /**
     * Bulk text: POST a list of taskIds → get back { taskId -> [paragraphs] }.
     */
    @PostMapping("/getText/bulk")
    @Operation(
        summary = "Bulk text retrieval",
        description = "POST a JSON array of taskIds and receive a map of { taskId -> [paragraphs] }."
    )
    public ResponseEntity<Map<String, List<String>>> getBulkText(@RequestBody List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, List<String>> response = new LinkedHashMap<>();
        for (String taskId : taskIds) {
            ExtractedData data = extractionStore.getResult(taskId);
            if (data == null || data.getContent() == null || data.getContent().isEmpty()) {
                response.put(taskId, List.of("No data available."));
            } else {
                List<String> paragraphs = Arrays.stream(data.getContent().split("\\n+"))
                        .map(String::trim)
                        .filter(s -> s.length() > 5)
                        .distinct()
                        .limit(100)
                        .collect(Collectors.toList());
                response.put(taskId, paragraphs.isEmpty() ? List.of("No significant text content.") : paragraphs);
            }
        }
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // DOWNLOAD ZIP  (per task)
    // ─────────────────────────────────────────────────────────────────────────────

    @GetMapping("/download/{taskId}")
    @Operation(summary = "Download ZIP of extracted data for a specific task", description = "Zips and downloads all saved extraction data for the given taskId.")
    public ResponseEntity<Resource> downloadZip(
            @Parameter(description = "Task ID returned from /extract") @PathVariable String taskId) {

        String storagePath = extractionStore.getStoragePath(taskId);
        if (storagePath == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path zipPath = zipService.zipDirectory(storagePath);
            Resource resource = new UrlResource(zipPath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + zipPath.getFileName().toString() + "\"")
                    .body(resource);
        } catch (Exception e) {
            MyLogger.err("Error creating zip for taskId " + taskId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
