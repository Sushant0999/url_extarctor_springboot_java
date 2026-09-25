package com.url.extractor;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
    info = @Info(
        title = "URL Extractor API",
        version = "1.0",
        description = "URL Data Extractor and Job Search API"
    )
)
public class UrlExtractorApplication {

    public static void main(String[] args) {
        Micronaut.run(UrlExtractorApplication.class, args);
    }
}
