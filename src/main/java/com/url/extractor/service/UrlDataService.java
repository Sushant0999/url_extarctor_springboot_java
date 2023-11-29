package com.url.extractor.service;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.url.extractor.model.UrlData;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UrlDataService {

    public static ArrayList<UrlData> data = new ArrayList<>();

    public static HashMap<String, List<String>> hashMap = new HashMap<>();

    //ADDING LIST TO HASHMAP
    public static void addDataToMap() {
        Iterator<UrlData> it = data.iterator();
        UrlData urlData = null;
        while (it.hasNext()) {
            urlData = it.next();
            urlData.setTopics(urlData.getTopics().replaceAll("[^a-zA-Z0-9\\s]", " "));
            hashMap.put(urlData.getBaseUrl(), urlData.getKeyword());
        }
    }

    //GETTING DATA FROM URL/WEBPAGE
    public static UrlData print(String url) {
        UrlData urlData = null;

        try (WebClient webClient = new WebClient()) {
            // Disable JavaScript (optional)
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);

//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode jsonNode = mapper.readTree(url);
//            String urlChecked = jsonNode.get("url").asText();
            // Fetch the web page
            HtmlPage page = webClient.getPage(url);

            //THIS IS WORKING
//            String headingText = page.getTitleText();
//            System.out.println("Heading: " + headingText);
            //ALL TEXT
//            System.out.println("htmlElement: " + page.getVisibleText());
//            LIST OF FORMS
//            System.out.println("PAGE : "+page.getForms());
            //DUMPING WEBSITE DATA
//            String name = "C:\\Users\\susha\\Desktop\\dump\\websiteData" + System.currentTimeMillis();
//            File file = new File(name);
//            page.getPage().save(file);
            //GETTING BASE URL
//            System.out.println(page.getBaseURL());
            System.out.println(page);
            // Extract multiple elements and iterate over them
            List<String> tags = new ArrayList<>();
            for (HtmlAnchor link : page.getAnchors()) {
                String linkText = link.toString();
                String linkUrl = link.getHrefAttribute();
//                System.out.println("Link: " + linkText + " - " + linkUrl);
                String temp = linkUrl.replaceAll("//", "");
                tags.add(temp);
            }
            urlData = new UrlData();
            urlData.setTopics(page.getTitleText().toLowerCase());
            urlData.setCategory(page.getContentType());
            urlData.setBaseUrl(String.valueOf(page.getBaseURL()));
            List<String> KeywordList = Arrays.stream(page.getVisibleText().toLowerCase().split(" ")).toList();
            urlData.setBaseUrl(page.getTitleText() + " : " + page.getVisibleText());
            urlData.setKeyword(KeywordList);
            urlData.setAnchorTags(tags);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return urlData;
    }

    public List<String> getBaseString() {
//        System.out.println(data.stream().collect(Collectors.groupingBy(UrlData::getTopics, Collectors.groupingBy(UrlData::getBaseString))));
//        System.out.println(data.stream().map(Collectors.groupingBy(UrlData::getBaseString),Collectors.));
        return data.stream().map(UrlData::getBaseString).collect(Collectors.toList());
    }

    //ADDING DATA TO LIST
    public ResponseEntity<Optional<UrlData>> insertData(String url) {
        UrlData urlData = UrlDataService.print(url);
        if (urlData == null) {
            System.out.println("DATA NOT FOUND");
            return ResponseEntity.noContent().build();
        }
        int index = data.size();
        urlData.setId(index);
        data.add(urlData);
        return ResponseEntity.ok(Optional.of(urlData));
    }

    public ResponseEntity<List<String>> insertBulk(List<String> list) {
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            UrlData urlData = UrlDataService.print(it.next());
            if (urlData == null) {
                System.out.println("DATA NOT FOUND");
                continue;
            }
            int index = data.size();
            urlData.setId(index);
            data.add(urlData);
        }
        return ResponseEntity.ok(list);
    }

    //GET ALL DATA
    public ResponseEntity<Optional<List<UrlData>>> getAllUrlData(Model m) {
        List<UrlData> urlData = data;
        if (urlData == null) {
            return ResponseEntity.noContent().build();
        }
        m.addAttribute("data", urlData);
        return ResponseEntity.ok(Optional.of(urlData));
    }

    //SEARCH BY TOPIC
    public ResponseEntity<Optional<List<String>>> searchByTopic(String topic) {
        List<String> urlData = new ArrayList<>();
        urlData = data.stream().map(UrlData::getTopics)
                .toList();
        if (urlData.size() == 0) {
            return ResponseEntity.noContent().build();
        }
        urlData.stream().filter(ele -> ele.contains(topic.replaceAll("[^a-zA-Z0-9\\s]", " "))).forEach(System.out::println);
        return ResponseEntity.ok(Optional.of(urlData.stream().filter(ele -> ele.contains(topic.replaceAll("[^a-zA-Z0-9\\s]", " "))).toList()));
    }

    //GET KEYWORD
    public ResponseEntity<Optional<Map<String, List<String>>>> getKeyword(String keyword) {
        System.out.println("DATA MAP SIZE : " + hashMap.size());
        HashMap<String, List<String>> result = new HashMap<>();
        UrlDataService.addDataToMap();
        if (hashMap.size() == 0) {
            System.out.println("NO DATA FOUND");
            return ResponseEntity.notFound().build();
        }
        for (Map.Entry<String, List<String>> entry : hashMap.entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            if (key.contains(keyword)) {
//                System.out.println("KEYWORD FOUND IN KEY");
//                System.out.println("KEY : " + key);
//                System.out.println("VALUE : " + value);
                result.put(key, value);
            } else if (value.contains(keyword)) {
//                System.out.println("KEYWORD FOUND IN VALUE");
//                System.out.println("KEY : " + key);
//                System.out.println("VALUE : " + value);
                result.put(key, value);
            }
        }
        if (result.size() == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.of(Optional.of(Optional.of(result)));
    }

    //GET ALL TAGS FROM
    public ResponseEntity<Optional<List<List<String>>>> getAllAnchorTags() {
        List<List<String>> urlData = new ArrayList<>();
        urlData = data.stream().map(UrlData::getAnchorTags).toList();
        if (urlData.size() == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Optional.of(urlData));
    }

    //SEARCH TAGS
    public ResponseEntity<List<List<String>>> getAllTag(String tag) {
        List<UrlData> urlData = null;
        try {
            urlData = data;
        } catch (Exception c) {
            return ResponseEntity.noContent().build();
        }
        List<List<String>> list = urlData.stream().map(UrlData::getAnchorTags).toList();
        if (list.size() == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(list);
    }

    //GETTING ALL TAGS FROM PAGE
    public ResponseEntity<List<String>> getTag(int id) {
        UrlData urlData = null;
        try {
            urlData = data.get(id);
        } catch (Exception c) {
            return ResponseEntity.noContent().build();
        }
        List<String> list = urlData.getAnchorTags();
        if (list.size() == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(list);
    }

}
