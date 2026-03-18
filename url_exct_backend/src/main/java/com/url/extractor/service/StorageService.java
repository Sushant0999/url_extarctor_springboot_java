package com.url.extractor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.extractor.dto.ExtractedData;
import com.url.extractor.utils.MyLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class StorageService {

    @Autowired
    private MediaService mediaService;

    private static final String BASE_STORAGE_PATH = "extracted_data";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String saveExtractedData(ExtractedData data) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String domain = getDomainName(data.getBaseUrl());
        String folderName = domain + "_" + timestamp;
        Path folderPath = Paths.get(BASE_STORAGE_PATH, folderName);

        try {
            Files.createDirectories(folderPath);
            Files.createDirectories(folderPath.resolve("images"));
            Files.createDirectories(folderPath.resolve("videos"));

            // Save JSON metadata
            saveJsonMetadata(data, folderPath);

            // Save main content
            saveTextContent(data.getContent(), folderPath.resolve("content.txt"));

            // Download and save media if links exist
            downloadAndSaveMedia(data, folderPath);

            MyLogger.info("Data saved successfully to: " + folderPath.toAbsolutePath());
            return folderPath.toAbsolutePath().toString();
        } catch (IOException e) {
            MyLogger.err("Failed to create storage folder: " + e.getMessage());
            return null;
        }
    }

    private void saveJsonMetadata(ExtractedData data, Path folderPath) {
        try {
            File metadataFile = folderPath.resolve("metadata.json").toFile();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile, data);
        } catch (IOException e) {
            MyLogger.err("Failed to save metadata.json: " + e.getMessage());
        }
    }

    private void saveTextContent(String content, Path filePath) {
        if (content == null) return;
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(content);
        } catch (IOException e) {
            MyLogger.err("Failed to save content.txt: " + e.getMessage());
        }
    }

    private void downloadAndSaveMedia(ExtractedData data, Path folderPath) {
        // Download images
        if (data.getImageUrls() != null && !data.getImageUrls().isEmpty()) {
            saveMediaFiles(data.getImageUrls(), folderPath.resolve("images"), "img");
        }

        // Download videos (links only for now, or binary if requested)
        if (data.getVideoUrls() != null && !data.getVideoUrls().isEmpty()) {
            saveMediaFiles(data.getVideoUrls(), folderPath.resolve("videos"), "vid");
        }
    }

    private void saveMediaFiles(List<String> urls, Path targetDir, String prefix) {
        int count = 1;
        for (String url : urls) {
            try {
                byte[] content = mediaService.downloadMedia(List.of(url), null).stream().findFirst().orElse(null);
                if (content != null) {
                    String extension = getExtensionFromUrl(url);
                    String fileName = prefix + "_" + count++ + extension;
                    try (FileOutputStream fos = new FileOutputStream(targetDir.resolve(fileName).toFile())) {
                        fos.write(content);
                    }
                }
            } catch (Exception e) {
                MyLogger.err("Failed to download/save media: " + url);
            }
        }
    }

    private String getDomainName(String url) {
        if (url == null) return "unknown";
        return url.replaceAll("https?://(www\\.)?", "").split("/")[0].replaceAll("[^a-zA-Z0-9]", "_");
    }

    private String getExtensionFromUrl(String url) {
        if (url == null || !url.contains(".")) return ".data";
        String ext = url.substring(url.lastIndexOf("."));
        if (ext.contains("?") || ext.contains("#")) {
            ext = ext.split("[?#]")[0];
        }
        return ext.length() > 5 ? ".data" : ext;
    }

    public void deleteTaskStorage(String folderPathStr) {
        if (folderPathStr == null || folderPathStr.isEmpty()) return;
        Path folderPath = Paths.get(folderPathStr);
        if (Files.exists(folderPath)) {
            try {
                Files.walk(folderPath)
                     .sorted((p1, p2) -> p2.compareTo(p1)) // reverse order to delete contents first
                     .map(Path::toFile)
                     .forEach(File::delete);
                MyLogger.info("Deleted task storage: " + folderPathStr);
            } catch (IOException e) {
                MyLogger.err("Failed to delete task storage directory: " + folderPathStr + " - " + e.getMessage());
            }
        }
    }
}
