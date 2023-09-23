package com.url.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.url.extractor.model.UrlData;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class UrlExtractorApplication {

    public static void main(String[] args) {
//        print();
        SpringApplication.run(UrlExtractorApplication.class, args);
    }
    public static UrlData print() {
        UrlData urlData = null;

        try (WebClient webClient = new WebClient()) {
            // Disable JavaScript (optional)
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);

//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode jsonNode = mapper.readTree(url);
//            String urlChecked = jsonNode.get("url").asText();
            // Fetch the web page
            String url = "http://5.39.88.99:18080/Better.Call.Saul.S02/";
            HtmlPage page = webClient.getPage(url);

            //THIS IS WORKING
//            String headingText = page.getTitleText();
//            System.out.println("Heading: " + headingText);
            //ALL TEXT
//            System.out.println("htmlElement: " + page.getVisibleText());
//            LIST OF FORMS
//            System.out.println("PAGE : "+page.getForms());
            //DUMPING WEBSITE DATA
            String name = "C:\\Users\\susha\\Desktop\\dump\\websiteData" + System.currentTimeMillis();
            File file = new File(name);
            page.getPage().save(file);
            //GETTING BASE URL
//            System.out.println(page.getBaseURL());

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

}
