package com.url.extractor.service;

import com.url.extractor.utils.MyLogger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ResumeParserService {

    @Autowired
    private GroqService groqService;

    public String parseResume(MultipartFile file) {
        String text = extractText(file);
        if (text == null || text.trim().isEmpty()) {
            MyLogger.err("ResumeParserService: No text extracted from file.");
            return null;
        }
        return groqService.parseResume(text);
    }

    private String extractText(MultipartFile file) {
        String filename = file.getOriginalFilename();
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
