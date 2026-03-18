package com.url.extractor.helper;

import com.url.extractor.dto.ExtractedData;

public interface ExtractionStrategy {

    ExtractedData extract(String url);
    String getName();

}
