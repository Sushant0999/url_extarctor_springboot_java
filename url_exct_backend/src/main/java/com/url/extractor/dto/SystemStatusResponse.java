package com.url.extractor.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Serdeable
public class SystemStatusResponse {
    private String status;
    private long uptime;
    private MemoryInfo memory;
    private TaskSummary tasks;
    private Map<String, String> components;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Serdeable
    public static class MemoryInfo {
        private long total;
        private long free;
        private long used;
        private String formatted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Serdeable
    public static class TaskSummary {
        private int total;
        private int pending;
        private int inProgress;
        private int completed;
        private int failed;
    }
}
