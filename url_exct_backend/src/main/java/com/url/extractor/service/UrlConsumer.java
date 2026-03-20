package com.url.extractor.service;

import com.url.extractor.config.RabbitConfig;
import com.url.extractor.dto.ExtractionMessage;
import com.url.extractor.utils.MyLogger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class UrlConsumer {

    @Autowired
    private ExtractionService extractionService;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void processUrl(ExtractionMessage message) {
        MyLogger.info("RabbitMQ Consumer: Received taskId -> " + message.getTaskId() + " for URL -> " + message.getUrl());
        extractionService.processSingleUrl(message.getTaskId(), message.getUrl());
    }
}