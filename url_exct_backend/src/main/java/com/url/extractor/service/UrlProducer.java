package com.url.extractor.service;

import com.url.extractor.config.RabbitConfig;
import com.url.extractor.dto.ExtractionMessage;
import com.url.extractor.utils.MyLogger;
import io.micronaut.context.annotation.Requires;
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@Requires(property = "app.rabbitmq.enabled", value = "true")
public class UrlProducer {

    @RabbitClient(RabbitConfig.EXCHANGE)
    public interface RabbitProducerClient {
        @Binding(RabbitConfig.ROUTING_KEY)
        void send(ExtractionMessage message);
    }

    @Inject
    private RabbitProducerClient client;

    public void sendUrl(String taskId, String url) {
        MyLogger.info("RabbitMQ Producer: Sending URL -> " + url + " with taskId -> " + taskId);
        client.send(new ExtractionMessage(taskId, url));
    }
}