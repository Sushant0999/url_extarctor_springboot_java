package com.url.extractor.controller;


import com.url.extractor.utils.MyLogger;
import com.url.extractor.utils.ZipDirectory;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.ui.Model;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.url.extractor.model.UrlData;
import com.url.extractor.service.UrlDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/urlData")
@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600L, methods = {RequestMethod.GET, RequestMethod.OPTIONS, RequestMethod.POST})
public class UrlDataController {

    @Autowired
    private UrlDataService urlDataService;


    @PostMapping("/insert")
    public ResponseEntity<?> insertBulk(@RequestBody List<String> urls, @RequestParam("jsEnable") Boolean jsEnable) {
        MyLogger.info("LINK ADDED " + urls.get(0) + " AND JS ENABLE " + jsEnable);
        UrlData data = null;
        try {
            data = urlDataService.getData(urls, jsEnable);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("SOMETHING WENT WRONG");
        }
        if (data == null) {
            return ResponseEntity.status(204).build();
        }
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/getTags")
    public ResponseEntity<?> getAllTags() {
        MyLogger.info("TAGS REQUESTED");
        List<String> list = urlDataService.getAllTags();
        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/search/{keyword}")
    public ResponseEntity<?> search(@PathVariable String keyword) {
        MyLogger.info("KEYWORD SEARCH REQUESTED");
        return ResponseEntity.ok().body(urlDataService.searchString(keyword));
    }

    @GetMapping("/getImages")
    public ResponseEntity<?> getImages() throws Exception {
        MyLogger.info("GET IMAGES REQUESTED");
        return ResponseEntity.of(Optional.ofNullable(urlDataService.images()));
    }

    @GetMapping("/getData")
    public ResponseEntity<?> getData() throws IOException {
        MyLogger.info("GET ZIP FILE REQUESTED");
        Resource file = urlDataService.sendZip();
        if (file == null) {
            MyLogger.warn("REQUEST NOT COMPLETED");
            return ResponseEntity.status(500).build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "data.zip");
        return ResponseEntity.ok()
                .headers(headers)
                .body(file);
    }

    @GetMapping("/getText")
    public ResponseEntity<?> getAllText(){
        List<String> stringList = urlDataService.textListStrings();
        if(stringList.isEmpty()){
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.ok().body(urlDataService.textListStrings());
    }
}
