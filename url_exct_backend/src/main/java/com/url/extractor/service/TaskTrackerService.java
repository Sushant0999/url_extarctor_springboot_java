package com.url.extractor.service;

import com.url.extractor.dto.ExtractedData;
import com.url.extractor.model.TaskStatus;
import com.url.extractor.utils.MyLogger;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskTrackerService {

    private final Map<String, TaskStatus> taskStatuses = new ConcurrentHashMap<>();
    private final Map<String, ExtractedData> taskResults = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void createTask(String taskId) {
        taskStatuses.put(taskId, TaskStatus.PENDING);
    }

    public void updateStatus(String taskId, TaskStatus status) {
        taskStatuses.put(taskId, status);
        MyLogger.info("Task [" + taskId + "] status updated to: " + status);
        broadcast(taskId, status.name());
    }

    public void completeTask(String taskId, ExtractedData result) {
        taskResults.put(taskId, result);
        updateStatus(taskId, TaskStatus.COMPLETED);
    }

    public void failTask(String taskId) {
        updateStatus(taskId, TaskStatus.FAILED);
    }

    public TaskStatus getStatus(String taskId) {
        return taskStatuses.get(taskId);
    }

    public ExtractedData getResult(String taskId) {
        return taskResults.get(taskId);
    }

    public SseEmitter subscribe(String taskId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(taskId, emitter);

        emitter.onCompletion(() -> emitters.remove(taskId));
        emitter.onTimeout(() -> emitters.remove(taskId));
        emitter.onError((e) -> emitters.remove(taskId));

        // Send initial status
        try {
            TaskStatus currentStatus = taskStatuses.getOrDefault(taskId, TaskStatus.PENDING);
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(currentStatus.name()));
        } catch (IOException e) {
            MyLogger.err("SSE Error during subscription for " + taskId + ": " + e.getMessage());
        }

        return emitter;
    }

    private void broadcast(String taskId, String status) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data(status));
                if (status.equals("COMPLETED") || status.equals("FAILED")) {
                    emitter.complete();
                    emitters.remove(taskId);
                }
            } catch (IOException e) {
                MyLogger.err("SSE Broadcast failed for " + taskId + ": " + e.getMessage());
                emitters.remove(taskId);
            }
        }
    }

    public void removeTask(String taskId) {
        taskStatuses.remove(taskId);
        taskResults.remove(taskId);
        emitters.remove(taskId);
    }
}
