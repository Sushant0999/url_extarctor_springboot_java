package com.url.extractor.model;


import java.util.List;

public class UrlData {

    private int id;
    private String topics;
    private String Category;
    private String baseUrl;
    private List<String> anchorTags;
    private List<String> keyword;

    @Override
    public String toString() {
        return "UrlData{" +
                "id=" + id +
                ", topics='" + topics + '\'' +
                ", Category='" + Category + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", anchorTags=" + anchorTags +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    public String getCategory() {
        return Category;
    }

    public void setCategory(String category) {
        Category = category;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }


    public List<String> getAnchorTags() {
        return anchorTags;
    }

    public void setAnchorTags(List<String> anchorTags) {
        this.anchorTags = anchorTags;
    }

    public List<String> getKeyword() {
        return keyword;
    }

    public void setKeyword(List<String> keyword) {
        this.keyword = keyword;
    }
}