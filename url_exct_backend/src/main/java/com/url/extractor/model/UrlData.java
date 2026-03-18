package com.url.extractor.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.util.List;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UrlData {

    private int id;
    private String topics;
    private String Category;
    private String baseUrl;
    private List<String> anchorTags;
    private String baseString;
    private List<String> keyword;
    private List<byte[]> images;

}