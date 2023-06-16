package com.url.extractor;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.host.dom.Document;
import com.url.extractor.model.UrlData;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@SpringBootApplication
public class UrlExtractorApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlExtractorApplication.class, args);
    }

}
