package com.url.extractor.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.url.extractor.dto.ExtractedData;
import com.url.extractor.service.*;
import com.url.extractor.utils.MyLogger;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.types.files.SystemFile;
import io.micronaut.http.sse.Event;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Controller("/urlData")
@Tag(name = "URL Extraction API", description = "Endpoints for background URL data extraction and status tracking. Supports multiple URLs in a single request.")
public class UrlDataController {

    @Inject
    private ExtractionService extractionService;

    @Inject
    private ExtractionStore extractionStore;

    @Inject
    private TaskTrackerService taskTrackerService;

    @Inject
    private MediaService mediaService;

    @Inject
    private ZipService zipService;

    @Get("/test")
    public HttpResponse<String> test() {
        return HttpResponse.ok("UrlDataController is active!");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // EXTRACT
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Accepts a single URL string OR an array of URLs.
     * Returns a map of { url -> taskId } for tracking each URL independently.
     */
    @Post(value = "/extract", consumes = {MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL})
    @Operation(
        summary = "Start URL extraction tasks",
        description = "Accepts a single URL string or a JSON array of URLs. Returns a map of { url -> taskId } for each submitted URL."
    )
    public HttpResponse<Map<String, String>> handleExtraction(@Body String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return HttpResponse.badRequest();
        }

        List<String> urls = new ArrayList<>();
        try {
            JsonNode input = new com.fasterxml.jackson.databind.ObjectMapper().readTree(rawBody);
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
                return HttpResponse.badRequest();
            }
        } catch (Exception e) {
            String trimmed = rawBody.trim().replace("\"", "");
            if (!trimmed.isEmpty()) {
                urls.add(trimmed);
            } else {
                return HttpResponse.badRequest();
            }
        }

        if (urls.isEmpty()) {
            return HttpResponse.badRequest();
        }

        Map<String, String> taskIdMap = extractionService.processBulk(urls);
        return HttpResponse.ok(taskIdMap);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // STATUS & RESULT (per task)
    // ─────────────────────────────────────────────────────────────────────────────

    @Get("/status/{taskId}")
    @Operation(summary = "Get task status", description = "Returns the current state of a task: PENDING, IN_PROGRESS, COMPLETED, or FAILED.")
    public HttpResponse<String> getStatus(
            @Parameter(description = "Task ID returned from /extract") @PathVariable String taskId) {
        var status = taskTrackerService.getStatus(taskId);
        return HttpResponse.ok(status != null ? status.name() : "NOT_FOUND");
    }

    @Get("/result/{taskId}")
    @Operation(summary = "Get extracted result for a single task", description = "Retrieves the full ExtractedData object for a completed task.")
    public HttpResponse<ExtractedData> getResult(
            @Parameter(description = "Task ID returned from /extract") @PathVariable String taskId) {
        ExtractedData result = taskTrackerService.getResult(taskId);
        return result != null ? HttpResponse.ok(result) : HttpResponse.notFound();
    }

    /**
     * Bulk status check: POST a JSON array of taskIds → get back { taskId -> status } map.
     */
    @Post("/status/bulk")
    @Operation(
        summary = "Bulk status check",
        description = "POST a JSON array of taskIds and receive a map of { taskId -> status } for all of them."
    )
    public HttpResponse<Map<String, String>> getBulkStatus(@Body List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return HttpResponse.badRequest();
        }
        Map<String, String> statusMap = new LinkedHashMap<>();
        for (String taskId : taskIds) {
            var status = taskTrackerService.getStatus(taskId);
            statusMap.put(taskId, status != null ? status.name() : "NOT_FOUND");
        }
        return HttpResponse.ok(statusMap);
    }

    /**
     * Bulk results: POST a JSON array of taskIds → get back { taskId -> ExtractedData } map.
     * Only tasks that are COMPLETED and have results are included.
     */
    @Post("/results/bulk")
    @Operation(
        summary = "Bulk results retrieval",
        description = "POST a JSON array of taskIds and receive a map of { taskId -> ExtractedData } for each completed task."
    )
    public HttpResponse<Map<String, ExtractedData>> getBulkResults(@Body List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return HttpResponse.badRequest();
        }
        Map<String, ExtractedData> resultsMap = new LinkedHashMap<>();
        for (String taskId : taskIds) {
            ExtractedData data = taskTrackerService.getResult(taskId);
            if (data != null) {
                resultsMap.put(taskId, data);
            }
        }
        return HttpResponse.ok(resultsMap);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SSE SUBSCRIPTION (per task)
    // ─────────────────────────────────────────────────────────────────────────────

    @Get(value = "/subscribe/{taskId}", produces = MediaType.TEXT_EVENT_STREAM)
    @Operation(summary = "Subscribe to task updates (SSE)", description = "Real-time Server-Sent Events stream for a specific task.")
    public Flux<Event<String>> subscribe(
            @Parameter(description = "Task ID to subscribe to") @PathVariable String taskId) {
        return taskTrackerService.subscribe(taskId);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // MEDIA (per task)
    // ─────────────────────────────────────────────────────────────────────────────

    @Get("/media/{taskId}")
    @Operation(
        summary = "Download media for a specific task",
        description = "Downloads all images and videos found during extraction for the given taskId."
    )
    public HttpResponse<?> getMedia(
            @Parameter(description = "Task ID returned from /extract") @PathVariable String taskId) {

        ExtractedData data = extractionStore.getResult(taskId);
        if (data == null) {
            return HttpResponse.badRequest("No extracted data available for taskId: " + taskId + ". Please run extraction first.");
        }

        List<String> imageUrls = data.getImageUrls();
        List<String> videoUrls = data.getVideoUrls();

        if ((imageUrls == null || imageUrls.isEmpty()) && (videoUrls == null || videoUrls.isEmpty())) {
            return HttpResponse.badRequest("No media links found for taskId: " + taskId);
        }

        try {
            List<byte[]> media = mediaService.downloadMedia(imageUrls, videoUrls);
            if (media.isEmpty()) {
                return HttpResponse.status(HttpStatus.NOT_FOUND).body("No media content could be downloaded for taskId: " + taskId);
            }
            return HttpResponse.ok(media);
        } catch (Exception e) {
            MyLogger.err("Error during media download for taskId " + taskId + ": " + e.getMessage());
            return HttpResponse.serverError("FAILED TO FETCH MEDIA CONTENT: " + e.getMessage());
        }
    }

    /**
     * Bulk media info: POST a list of taskIds → get back { taskId -> { imageUrls, videoUrls } }.
     */
    @Post("/media/bulk")
    @Operation(
        summary = "Bulk media URL retrieval",
        description = "POST a JSON array of taskIds and get back a map of { taskId -> { imageUrls, videoUrls } }."
    )
    public HttpResponse<Map<String, Map<String, List<String>>>> getBulkMedia(@Body List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return HttpResponse.badRequest();
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
        return HttpResponse.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TAGS (per task)
    // ─────────────────────────────────────────────────────────────────────────────

    @Get("/getTags/{taskId}")
    @Operation(summary = "Get anchor tags for a specific task", description = "Returns all anchor/href tags extracted from the page for the given taskId.")
    public HttpResponse<List<String>> getTags(
            @Parameter(description = "Task ID returned from /extract") @PathVariable String taskId) {
        ExtractedData data = extractionStore.getResult(taskId);
        if (data == null || data.getAnchorTags() == null) {
            return HttpResponse.ok(List.of());
        }
        return HttpResponse.ok(data.getAnchorTags());
    }

    /**
     * Bulk tags: POST a list of taskIds → get back { taskId -> [anchorTags] }.
     */
    @Post("/getTags/bulk")
    @Operation(
        summary = "Bulk anchor tags retrieval",
        description = "POST a JSON array of taskIds and receive a map of { taskId -> [anchorTags] }."
    )
    public HttpResponse<Map<String, List<String>>> getBulkTags(@Body List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return HttpResponse.badRequest();
        }
        Map<String, List<String>> response = new LinkedHashMap<>();
        for (String taskId : taskIds) {
            ExtractedData data = extractionStore.getResult(taskId);
            response.put(taskId, (data != null && data.getAnchorTags() != null) ? data.getAnchorTags() : List.of());
        }
        return HttpResponse.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEXT (per task)
    // ─────────────────────────────────────────────────────────────────────────────

    @Get("/getText/{taskId}")
    @Operation(summary = "Get page text for a specific task", description = "Returns meaningful text paragraphs extracted from the page for the given taskId.")
    public HttpResponse<List<String>> getText(
            @Parameter(description = "Task ID returned from /extract") @PathVariable String taskId) {
        ExtractedData data = extractionStore.getResult(taskId);
        if (data == null || data.getContent() == null || data.getContent().isEmpty()) {
            return HttpResponse.ok(List.of("No data available for taskId: " + taskId));
        }

        List<String> paragraphs = Arrays.stream(data.getContent().split("\\n+"))
                .map(String::trim)
                .filter(s -> s.length() > 5)
                .distinct()
                .limit(100)
                .collect(Collectors.toList());

        return HttpResponse.ok(
                paragraphs.isEmpty() ? List.of("No significant text content detected.") : paragraphs);
    }

    /**
     * Bulk text: POST a list of taskIds → get back { taskId -> [paragraphs] }.
     */
    @Post("/getText/bulk")
    @Operation(
        summary = "Bulk text retrieval",
        description = "POST a JSON array of taskIds and receive a map of { taskId -> [paragraphs] }."
    )
    public HttpResponse<Map<String, List<String>>> getBulkText(@Body List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return HttpResponse.badRequest();
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
        return HttpResponse.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // DOWNLOAD ZIP (per task)
    // ─────────────────────────────────────────────────────────────────────────────

    @Get(value = "/download/{taskId}", produces = MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Download ZIP of extracted data for a specific task", description = "Zips and downloads all saved extraction data for the given taskId.")
    public HttpResponse<SystemFile> downloadZip(
            @Parameter(description = "Task ID returned from /extract") @PathVariable String taskId) {

        String storagePath = extractionStore.getStoragePath(taskId);
        if (storagePath == null) {
            return HttpResponse.badRequest();
        }

        try {
            Path zipPath = zipService.zipDirectory(storagePath);
            SystemFile file = new SystemFile(zipPath.toFile()).attach(zipPath.getFileName().toString());
            return HttpResponse.ok(file);
        } catch (Exception e) {
            MyLogger.err("Error creating zip for taskId " + taskId + ": " + e.getMessage());
            return HttpResponse.serverError();
        }
    }
}
