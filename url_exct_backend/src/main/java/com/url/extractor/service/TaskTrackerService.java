package com.url.extractor.service;

import com.url.extractor.dto.ExtractedData;
import com.url.extractor.model.TaskStatus;
import com.url.extractor.utils.MyLogger;
import io.micronaut.http.sse.Event;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class TaskTrackerService {

    private final Map<String, TaskStatus> taskStatuses = new ConcurrentHashMap<>();
    private final Map<String, ExtractedData> taskResults = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<Event<String>>> emitters = new ConcurrentHashMap<>();

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

    public Flux<Event<String>> subscribe(String taskId) {
        Sinks.Many<Event<String>> sink = emitters.computeIfAbsent(
                taskId,
                k -> Sinks.many().multicast().directBestEffort()
        );

        TaskStatus currentStatus = taskStatuses.getOrDefault(taskId, TaskStatus.PENDING);

        return Flux.concat(
                Flux.just(Event.of(currentStatus.name()).name("status")),
                sink.asFlux()
        ).doFinally(signalType -> emitters.remove(taskId));
    }

    private void broadcast(String taskId, String status) {
        Sinks.Many<Event<String>> sink = emitters.get(taskId);
        if (sink != null) {
            try {
                sink.tryEmitNext(Event.of(status).name("status"));
                if (status.equals("COMPLETED") || status.equals("FAILED")) {
                    sink.tryEmitComplete();
                    emitters.remove(taskId);
                }
            } catch (Exception e) {
                MyLogger.err("SSE Broadcast failed for " + taskId + ": " + e.getMessage());
                emitters.remove(taskId);
            }
        }
    }

    public void removeTask(String taskId) {
        taskStatuses.remove(taskId);
        taskResults.remove(taskId);
        Sinks.Many<Event<String>> sink = emitters.remove(taskId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }

    public Map<TaskStatus, Integer> getStatusCounts() {
        Map<TaskStatus, Integer> counts = new HashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            counts.put(status, 0);
        }
        for (TaskStatus status : taskStatuses.values()) {
            counts.put(status, counts.get(status) + 1);
        }
        return counts;
    }

    public int totalTasksCount() {
        return taskStatuses.size();
    }
}
