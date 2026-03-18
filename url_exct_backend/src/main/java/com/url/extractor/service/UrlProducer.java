package com.url.extractor.service;

import com.url.extractor.config.RabbitConfig;
import com.url.extractor.dto.ExtractionMessage;
import com.url.extractor.utils.MyLogger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UrlProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendUrl(String taskId, String url) {
        MyLogger.info("RabbitMQ Producer: Sending URL -> " + url + " with taskId -> " + taskId);
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY,
                new ExtractionMessage(taskId, url)
        );
    }
}