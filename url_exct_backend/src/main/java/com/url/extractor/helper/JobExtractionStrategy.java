package com.url.extractor.helper;

import com.url.extractor.dto.JobDto;
import java.util.List;

public interface JobExtractionStrategy {
    List<JobDto> extract(String url);
    String getName();
}
