package com.url.extractor.controller;

import com.url.extractor.dto.SystemStatusResponse;
import com.url.extractor.model.TaskStatus;
import com.url.extractor.service.TaskTrackerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/actuator/frontend")
@CrossOrigin(origins = "*")
@Tag(name = "Actuator for Frontend", description = "Consolidated system status and health for the dashboard.")
public class SystemStatusController {

    @Autowired
    private TaskTrackerService taskTrackerService;

    @GetMapping("/status")
    @Operation(summary = "Get application and system overview")
    public ResponseEntity<SystemStatusResponse> getSystemStatus() {
        SystemStatusResponse status = SystemStatusResponse.builder()
                .status("UP")
                .uptime(ManagementFactory.getRuntimeMXBean().getUptime())
                .memory(getMemoryInfo())
                .tasks(getTaskSummary())
                .components(getComponentStatus())
                .build();
        return ResponseEntity.ok(status);
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
        components.put("database", "DISABLED"); // No DB in this project?
        components.put("rabbitmq", "UP"); // Assuming RabbitMQ is reachable
        return components;
    }
}
