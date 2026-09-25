package com.url.extractor.service;

import com.url.extractor.config.RabbitConfig;
import com.url.extractor.dto.ExtractionMessage;
import com.url.extractor.utils.MyLogger;
import io.micronaut.context.annotation.Requires;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import jakarta.inject.Inject;

@RabbitListener
@Requires(property = "app.rabbitmq.enabled", value = "true")
public class UrlConsumer {

    @Inject
    private ExtractionService extractionService;

    @Queue(RabbitConfig.QUEUE)
    public void processUrl(ExtractionMessage message) {
        MyLogger.info("RabbitMQ Consumer: Received taskId -> " + message.getTaskId() + " for URL -> " + message.getUrl());
        extractionService.processSingleUrl(message.getTaskId(), message.getUrl());
    }
}