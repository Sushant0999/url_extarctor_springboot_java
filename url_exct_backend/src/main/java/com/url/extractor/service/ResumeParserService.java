package com.url.extractor.service;

import com.url.extractor.utils.MyLogger;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;

@Singleton
public class ResumeParserService {

    @Inject
    private GroqService groqService;

    public String parseResume(CompletedFileUpload file) {
        String text = extractText(file);
        if (text == null || text.trim().isEmpty()) {
            MyLogger.err("ResumeParserService: No text extracted from file.");
            return null;
        }
        return groqService.parseResume(text);
    }

    private String extractText(CompletedFileUpload file) {
        String filename = file.getFilename();
        if (filename == null) return null;

        try (InputStream is = file.getInputStream()) {
            if (filename.toLowerCase().endsWith(".pdf")) {
                return extractTextFromPdf(is);
            } else if (filename.toLowerCase().endsWith(".docx")) {
                return extractTextFromDocx(is);
            } else {
                MyLogger.err("ResumeParserService: Unsupported file format: " + filename);
                return null;
            }
        } catch (IOException e) {
            MyLogger.err("ResumeParserService: File reading error: " + e.getMessage());
            return null;
        }
    }

    private String extractTextFromPdf(InputStream is) throws IOException {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromDocx(InputStream is) throws IOException {
        try (XWPFDocument document = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
