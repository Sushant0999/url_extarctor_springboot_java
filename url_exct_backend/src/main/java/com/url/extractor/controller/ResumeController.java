package com.url.extractor.controller;

import com.url.extractor.service.ResumeParserService;
import com.url.extractor.utils.MyLogger;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;

@Controller("/api/resume")
@Tag(name = "Resume API", description = "Endpoints for parsing resumes and extracting job search filters.")
public class ResumeController {

    @Inject
    private ResumeParserService resumeParserService;

    @Post(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Parse resume", description = "Extracts job title, skills, and experience level from an uploaded PDF or DOCX resume using AI.")
    public HttpResponse<String> parseResume(@Part("file") CompletedFileUpload file) {
        if (file == null || file.getSize() == 0) {
            return HttpResponse.badRequest("File is empty.");
        }

        MyLogger.info("ResumeController: Received resume upload: " + file.getFilename());
        
        String result = resumeParserService.parseResume(file);
        
        if (result == null) {
            return HttpResponse.serverError("Failed to parse resume.");
        }

        return HttpResponse.ok(result);
    }
}
