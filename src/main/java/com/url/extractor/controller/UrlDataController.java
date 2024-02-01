package com.url.extractor.controller;


import org.springframework.ui.Model;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.url.extractor.model.UrlData;
import com.url.extractor.service.UrlDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/urlData")
@CrossOrigin(origins = "*")
public class UrlDataController {

    @Autowired
    private UrlDataService urlDataService;


    @PostMapping("/insert")
    public ResponseEntity<?> insertBulk(@RequestBody List<String> urls) {
        System.out.println("URL INSERTED " + urls.get(0));
        try{
            urlDataService.getData(urls);
        }catch (Exception e){
           return ResponseEntity.internalServerError().body("SOMETHING WENT WRONG");
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/getTags")
    public ResponseEntity<?> getAllTags(){
        List<List<String>> list = urlDataService.getAllTags();
        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/search/{keyword}")
    public ResponseEntity<?> search(@PathVariable String keyword){
        return ResponseEntity.ok().body(urlDataService.searchString(keyword));
    }

    @GetMapping("/getImages")
    public ResponseEntity<?> getImages() throws Exception {
        return ResponseEntity.of(Optional.ofNullable(urlDataService.images()));
    }


}
