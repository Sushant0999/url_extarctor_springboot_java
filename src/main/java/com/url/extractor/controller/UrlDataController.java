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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/urlData")
public class UrlDataController {

    @Autowired
    private UrlDataService urlDataService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/insert")
    public ResponseEntity<?> insert(@RequestParam("baseUrl") String url, Model m) {
        m.addAttribute("urlData",urlDataService.insertData(url));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/insertBulk")
    public ResponseEntity insertBulk(@RequestBody String links) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(links, JsonObject.class);
        JsonArray urlArray = jsonObject.getAsJsonArray("url");
        int i = 0;
        try {
            while (i != urlArray.size()) {
                if (urlArray != null && urlArray.size() > 0) {
                    JsonElement jsonElement = urlArray.get(i);
                    if (jsonElement.isJsonPrimitive()) {
                        String url = jsonElement.getAsString();
                        JsonObject newJsonObject = new JsonObject();
                        newJsonObject.addProperty("url", url);
                        String newJson = gson.toJson(newJsonObject);
                        urlDataService.insertData(newJson);
                        System.out.println("DATA ADDED " + (i + 1));
                    }
                }
                i++;
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/getAll")
    public ResponseEntity<Optional<List<UrlData>>> getAll(@ModelAttribute Model m) {
        return urlDataService.getAllUrlData(m);
    }

    @GetMapping("/searchByTopic/{topic}")
    public ResponseEntity<Optional<List<String>>> searchByTopic(@PathVariable String topic) {
        return urlDataService.searchByTopic(topic);
    }

    @GetMapping("/getAnchorTags")
    public ResponseEntity<Optional<List<List<String>>>> getTag() {
        return urlDataService.getAllAnchorTags();
    }

    @GetMapping("/getById/{id}")
    public ResponseEntity<List<String>> getTag(@PathVariable int id) {
        return urlDataService.getTag(id);
    }

    @GetMapping("/getAllTag")
    public ResponseEntity<List<List<String>>> getAllTag(String tag) {
        return urlDataService.getAllTag(tag);
    }

    @GetMapping("/getBase")
    public List<String> getBaseString() {
        return urlDataService.getBaseString();
    }

    @GetMapping("/search/{keyword}")
    public ResponseEntity<List<String>> getkeyword(@PathVariable String keyword) {
        List<String> values = new ArrayList<>();
        ResponseEntity<Optional<Map<String, List<String>>>> optionalValue = urlDataService.getKeyword(keyword);
        if (optionalValue.getStatusCode() == HttpStatusCode.valueOf(200)) {
            Optional<Map<String, List<String>>> mapValue = optionalValue.getBody();
            if (mapValue.isPresent()) {
                Map<String, List<String>> map = mapValue.get();

                // Access and work with the map here
//                for (String key : map.keySet()) {
//                    values = map.get(key);
//                    System.out.println("Key: " + key);
//                }

            } else {
                // Handle the case where the Optional is empty
                System.out.println("No map value present.");
                return ResponseEntity.noContent().build();
            }
            // Further extraction or processing
        }
        return ResponseEntity.ok(values);
    }

}
