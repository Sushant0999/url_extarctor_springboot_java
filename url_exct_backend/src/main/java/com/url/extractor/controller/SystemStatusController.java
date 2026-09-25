package com.url.extractor.controller;

import com.url.extractor.dto.SystemStatusResponse;
import com.url.extractor.model.TaskStatus;
import com.url.extractor.service.TaskTrackerService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

@Controller
@Tag(name = "Actuator for Frontend", description = "Consolidated system status and health for the dashboard.")
public class SystemStatusController {

    @Inject
    private TaskTrackerService taskTrackerService;

    @Get("/actuator/frontend/status")
    @Operation(summary = "Get application and system overview")
    public HttpResponse<SystemStatusResponse> getSystemStatus() {
        SystemStatusResponse status = SystemStatusResponse.builder()
                .status("UP")
                .uptime(ManagementFactory.getRuntimeMXBean().getUptime())
                .memory(getMemoryInfo())
                .tasks(getTaskSummary())
                .components(getComponentStatus())
                .build();
        return HttpResponse.ok(status);
    }

    @Get("/actuator/health")
    @Operation(summary = "Actuator Health check")
    public HttpResponse<Map<String, Object>> getActuatorHealth() {
        return HttpResponse.ok(Map.of("status", "UP"));
    }

    @Get("/health")
    @Operation(summary = "Health check")
    public HttpResponse<Map<String, Object>> getHealth() {
        return HttpResponse.ok(Map.of("status", "UP"));
    }

    @Get("/actuator/info")
    @Operation(summary = "Application info")
    public HttpResponse<Map<String, Object>> getInfo() {
        return HttpResponse.ok(Map.of(
                "app", Map.of("name", "urlExtractor", "framework", "Micronaut 4.7.6"),
                "status", "UP"
        ));
    }

    @Get("/actuator/metrics")
    @Operation(summary = "Application metrics")
    public HttpResponse<Map<String, Object>> getMetrics() {
        Runtime runtime = Runtime.getRuntime();
        return HttpResponse.ok(Map.of(
                "totalMemoryMB", runtime.totalMemory() / (1024 * 1024),
                "freeMemoryMB", runtime.freeMemory() / (1024 * 1024),
                "activeTasks", taskTrackerService.totalTasksCount()
        ));
    }

    private SystemStatusResponse.MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;
        
        return SystemStatusResponse.MemoryInfo.builder()
                .total(total / (1024 * 1024))
                .free(free / (1024 * 1024))
                .used(used / (1024 * 1024))
                .formatted(String.format("%dMB / %dMB used", used / (1024 * 1024), total / (1024 * 1024)))
                .build();
    }

    private SystemStatusResponse.TaskSummary getTaskSummary() {
        Map<TaskStatus, Integer> counts = taskTrackerService.getStatusCounts();
        int total = taskTrackerService.totalTasksCount();
        
        return SystemStatusResponse.TaskSummary.builder()
                .total(total)
                .pending(counts.getOrDefault(TaskStatus.PENDING, 0))
                .inProgress(counts.getOrDefault(TaskStatus.IN_PROGRESS, 0))
                .completed(counts.getOrDefault(TaskStatus.COMPLETED, 0))
                .failed(counts.getOrDefault(TaskStatus.FAILED, 0))
                .build();
    }

    private Map<String, String> getComponentStatus() {
        Map<String, String> components = new HashMap<>();
        components.put("database", "DISABLED"); // No DB in this project
        components.put("rabbitmq", "UP");
        return components;
    }
}
