package com.url.extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchFilter {
    private String query;
    private List<String> locations;
    private String jobType;
    private Integer distance;
    private Integer datePosted;
    private List<String> skills;
    private String additionalKeywords;
    private String workMode; // remote, hybrid, onsite
    private String experienceLevel; // entry_level, mid_level, senior_level
    private String country; // 'in' for India, 'us' for USA, etc.
    private List<String> platforms; // ["indeed", "linkedin", "naukri"]
    private List<String> companies; // ["Google", "TCS"]
    private Integer page; // Current page to search
}
