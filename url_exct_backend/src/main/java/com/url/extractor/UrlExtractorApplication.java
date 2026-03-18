package com.url.extractor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UrlExtractorApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlExtractorApplication.class, args);
    }

}
