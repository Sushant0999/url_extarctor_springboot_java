package com.url.extractor.controller;

import com.url.extractor.service.ResumeParserService;
import com.url.extractor.utils.MyLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600L, methods = { RequestMethod.POST, RequestMethod.OPTIONS })
@Tag(name = "Resume API", description = "Endpoints for parsing resumes and extracting job search filters.")
public class ResumeController {

    @Autowired
    private ResumeParserService resumeParserService;

    @PostMapping("/parse")
    @Operation(summary = "Parse resume", description = "Extracts job title, skills, and experience level from an uploaded PDF or DOCX resume using AI.")
    public ResponseEntity<String> parseResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty.");
        }

        MyLogger.info("ResumeController: Received resume upload: " + file.getOriginalFilename());
        
        String result = resumeParserService.parseResume(file);
        
        if (result == null) {
            return ResponseEntity.internalServerError().body("Failed to parse resume.");
        }

        return ResponseEntity.ok(result);
    }
}
