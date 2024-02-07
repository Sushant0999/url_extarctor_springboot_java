package com.url.extractor.service;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.url.extractor.model.UrlData;
import com.url.extractor.utils.MyLogger;
import com.url.extractor.utils.ZipDirectory;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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

    public static ArrayList<UrlData> data = null;

    public static List<String> imageList = null;

    public static String pageName = "";


    //GETTING DATA FROM URL/WEBPAGE
    public static UrlData print(String url, boolean jsEnable) throws IOException {
        UrlData urlData = null;
        data = new ArrayList<>();

        try {
            String filePath = System.getProperty("user.dir");
            FileUtils.deleteDirectory(new File(filePath + "\\data\\temp"));
            FileUtils.deleteQuietly(new File(filePath + "\\data\\" + "temp.zip"));
            MyLogger.info("REMOVED OLD FILES");
        } catch (Exception e) {
            MyLogger.info("NO DIRECTORY TO REMOVE");
        }

        try (WebClient webClient = new WebClient()) {
            // Disable JavaScript (optional)
            webClient.getOptions().setJavaScriptEnabled(!jsEnable);
            webClient.getOptions().setCssEnabled(false);
            // Fetch the web page
            HtmlPage page = webClient.getPage(url);
            //THIS IS WORKING
            String headingText = page.getTitleText();
            //ALL TEXT
//            System.out.println("htmlElement: " + page.getVisibleText());
//            LIST OF FORMS
//            System.out.println("PAGE : "+page.getForms());
            //DUMPING WEBSITE DATA
            //System.getProperties().getProperty("java.class.path").split(";")[0]  + "\\data\\"
            String filePath = System.getProperty("user.dir");
            pageName = filePath + "\\data\\temp\\" + System.currentTimeMillis();
            File file = new File(pageName);
            page.getPage().save(file);
            //GETTING BASE URL
//            page.getBaseURL();
//            System.out.println(page.getBody());
            // Extract multiple elements and iterate over them
            List<String> tags = new ArrayList<>();
            for (HtmlAnchor link : page.getAnchors()) {
                String linkUrl = link.getHrefAttribute();
                if(linkUrl.contains("https")){
                    tags.add(linkUrl);
                }
            }
            urlData = new UrlData();
            urlData.setTopics(page.getTitleText().toLowerCase());
            urlData.setCategory(page.getContentType());
            urlData.setBaseUrl(String.valueOf(page.getBaseURL()));
            List<String> KeywordList = Arrays.stream(page.getVisibleText().toLowerCase().split(" ")).toList();
            urlData.setBaseUrl(page.getTitleText() + " : " + page.getVisibleText());
            urlData.setKeyword(KeywordList);
            urlData.setAnchorTags(tags);
            data.add(urlData);

        } catch (Exception e) {
            MyLogger.err(e.getMessage());
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
    public UrlData getData(List<String> urls, boolean jsEnable) throws Exception {
        UrlData data = null;
        try {
            for (String url : urls) {
                data = print(url, jsEnable);
            }
        } catch (Exception e) {
            MyLogger.err(e.getMessage());
            throw new Exception("SOMETHING WENT WRONG");
        }
        return data;
    }

    //GET ALL ANCHOR TAGS
    public List<String> getAllTags() {
        return data.stream()
                .map(UrlData::getAnchorTags) // Map each UrlData object to its list of anchor tags
                .flatMap(List::stream) // Flatten the lists of anchor tags into a single stream
                .distinct() // Ensure uniqueness of anchor tags
                .collect(Collectors.toList()); // Collect the unique anchor tags into a list
    }

    public boolean searchString(String s) {
//        System.out.println(data.stream()
//                .anyMatch(urlData -> urlData.getKeyword()
//                        .stream()
//                        .anyMatch(keyword -> keyword.equalsIgnoreCase(s))));
        return data.stream()
                .anyMatch(urlData -> urlData.getKeyword()
                        .stream()
                        .anyMatch(keyword -> keyword.equalsIgnoreCase(s)));
    }

    public List<String> getImages() throws IOException {
        imageList = new ArrayList<>();
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
            });

            fileList.close();
        } catch (IOException e) {
            MyLogger.err(e.getMessage());
        }
        return imageList;
    }

    public Set<byte[]> images() throws Exception {
        getImages();
        if (imageList.isEmpty()) {
            return new HashSet<>();
        }
        Set<byte[]> images = new HashSet<>();
        try {
            for (String s : imageList) {
                File file = new File(s);
                byte[] fileContent = Files.readAllBytes(file.toPath());
                images.add(fileContent);
            }
        } catch (Exception e) {
            MyLogger.err(e.getMessage());
            throw new Exception("SOMETHING WENT WRONG");
        }
        return images;
    }

    public Resource sendZip() {
        try {
            ZipDirectory.zip();
            String filePath = System.getProperty("user.dir");
            File directoryPath = new File(filePath + "\\data\\");

            Path paths = Paths.get(directoryPath.toURI()).resolve("temp.zip").normalize();
            return new UrlResource(paths.toUri());
        } catch (IOException e) {
            MyLogger.err(e.getMessage() + "\n" + "UNABLE TO PROCESS REQUEST");
            return null;
        }
    }
}
