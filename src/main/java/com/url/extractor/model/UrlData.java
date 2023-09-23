package com.url.extractor.model;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
@Getter
@Setter
@ToString
public class UrlData {

    private int id;
    private String topics;
    private String Category;
    private String baseUrl;
    private List<String> anchorTags;
    private String baseString;
    private List<String> keyword;

}