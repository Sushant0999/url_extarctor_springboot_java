package com.url.extractor.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Serdeable
public class ExtractionMessage {
    private String taskId;
    private String url;
}
