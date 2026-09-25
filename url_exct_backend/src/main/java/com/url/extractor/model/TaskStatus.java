package com.url.extractor.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
