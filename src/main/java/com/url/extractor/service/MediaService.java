package com.url.extractor.service;

import com.url.extractor.utils.MyLogger;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MediaService {

    public List<byte[]> downloadMedia(List<String> imageUrls, List<String> videoUrls) {
        List<String> allUrls = new ArrayList<>();
        if (imageUrls != null) allUrls.addAll(imageUrls);
        if (videoUrls != null) allUrls.addAll(videoUrls);
        
        List<byte[]> mediaContent = new ArrayList<>();
        
        if (allUrls.isEmpty()) {
            return mediaContent;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            for (String url : allUrls) {
                try {
                    HttpGet request = new HttpGet(url);
                    byte[] contentBytes = httpClient.execute(request, response -> {
                        if (response.getCode() == 200) {
                            return EntityUtils.toByteArray(response.getEntity());
                        }
                        return null;
                    });
                    
                    if (contentBytes != null) {
                        mediaContent.add(contentBytes);
                    }
                } catch (Exception e) {
                    MyLogger.err("Failed to download media from: " + url + " | Error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            MyLogger.err("Error initializing HttpClient: " + e.getMessage());
        }
        
        return mediaContent;
    }
}
