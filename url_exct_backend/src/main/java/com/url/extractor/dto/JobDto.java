package com.url.extractor.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Serdeable
public class JobDto {
    private String title;
    private String company;
    private String location;
    private String link;
    private String salary;
    private String description;
    private String datePosted;
    private String source;
}
