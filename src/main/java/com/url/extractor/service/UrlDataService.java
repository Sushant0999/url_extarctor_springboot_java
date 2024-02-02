package com.url.extractor.service;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.url.extractor.model.UrlData;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Locale.filter;

@Service
public class UrlDataService {

    public static ArrayList<UrlData> data = new ArrayList<>();

    public static List<String> imageList = new ArrayList<>();


    public static String pageName = "";


    //GETTING DATA FROM URL/WEBPAGE
    public static UrlData print(String url) throws IOException {
        UrlData urlData = null;

        try {
            String filePath = System.getProperty("user.dir");
            FileUtils.deleteDirectory(new File(filePath + "\\data\\temp"));
            System.out.println("DIRECTORY REMOVED");
        } catch (Exception e) {
            System.out.println("NO DIRECTORY TO REMOVE");
        }

        try (WebClient webClient = new WebClient()) {
            // Disable JavaScript (optional)
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            // Fetch the web page
            HtmlPage page = webClient.getPage(url);
            //THIS IS WORKING
            String headingText = page.getTitleText();
//            System.out.println("Heading: " + headingText);
            //ALL TEXT
//            System.out.println("htmlElement: " + page.getVisibleText());
//            LIST OF FORMS
//            System.out.println("PAGE : "+page.getForms());
            //DUMPING WEBSITE DATA
            //System.getProperties().getProperty("java.class.path").split(";")[0]  + "\\data\\"
            String filePath = System.getProperty("user.dir");
            System.out.println(filePath);
            pageName = filePath + "\\data\\temp\\" + System.currentTimeMillis();
            File file = new File(pageName);
            page.getPage().save(file);
            //GETTING BASE URL
//            System.out.println(page.getBaseURL());
            // Extract multiple elements and iterate over them
            List<String> tags = new ArrayList<>();
            for (HtmlAnchor link : page.getAnchors()) {
                String linkText = link.toString();
                String linkUrl = link.getHrefAttribute();
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
            if (data.size() > 10) {
                data.remove(10);
                data.add(0, urlData);
            } else {
                data.add(0, urlData);
            }


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return urlData;
    }

    public static Optional<Boolean> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> {
                    String extension = f.substring(f.lastIndexOf(".") + 1);
                    return extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("jpg") ||
                            extension.equalsIgnoreCase("jpeg") || extension.equalsIgnoreCase("gif") ||
                            extension.equalsIgnoreCase("svg");
                });
    }

    //INSERTING MULTIPLE URLS
    public UrlData getData(List<String> urls) throws Exception {
        UrlData data = null;
        try {
            for (String url : urls) {
              data = print(url);
            }
        } catch (Exception e) {
            throw new Exception("SOMETHING WENT WRONG");
        }
        if(data == null){
            return null;
        }else{
            return data;
        }
    }

    //GET ALL ANCHOR TAGS
    public List<List<String>> getAllTags() {
        return data.stream().map(UrlData::getAnchorTags).collect(Collectors.toList());
    }

    public boolean searchString(String s) {
        System.out.println(data.stream()
                .anyMatch(urlData -> urlData.getKeyword()
                        .stream()
                        .anyMatch(keyword -> keyword.equalsIgnoreCase(s))));
        return data.stream()
                .anyMatch(urlData -> urlData.getKeyword()
                        .stream()
                        .anyMatch(keyword -> keyword.equalsIgnoreCase(s)));
    }

    public List<String> getImages() throws IOException {
        String filePath = System.getProperty("user.dir");
        File directoryPath = new File(filePath + "\\data\\temp\\");
        String dirPath = Arrays.toString(directoryPath.list());
        try {
            Stream<Path> fileList = Files.walk(Paths.get(filePath + "\\data\\temp\\" + dirPath.split(",")[0].substring(1)));

            fileList.forEach(path -> {
                Optional<Boolean> isImage = getExtensionByStringHandling(path.toString());
                if (isImage.isPresent() && isImage.get()) {
                    imageList.add(path.toString());
                }
//                else {
//                    System.out.println("Non-image File: " + path.toString());
//                }
            });

            fileList.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageList;
    }

    public List<byte[]> images() throws Exception {
        getImages();
        if (imageList.isEmpty()) {
            return new ArrayList<>();
        }
        List<byte[]> images = new ArrayList<>();
        try{
            for (String s : imageList) {
                File file = new File(s);
                byte[] fileContent = Files.readAllBytes(file.toPath());
                images.add(fileContent);
            }
        }catch (Exception e){
            throw new Exception("SOMETHING WENT WRONG");
        }
        return images;
    }
}
