package com.url.extractor.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Serdeable
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

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public List<String> getLocations() { return locations; }
    public void setLocations(List<String> locations) { this.locations = locations; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public Integer getDistance() { return distance; }
    public void setDistance(Integer distance) { this.distance = distance; }

    public Integer getDatePosted() { return datePosted; }
    public void setDatePosted(Integer datePosted) { this.datePosted = datePosted; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public String getAdditionalKeywords() { return additionalKeywords; }
    public void setAdditionalKeywords(String additionalKeywords) { this.additionalKeywords = additionalKeywords; }

    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }

    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) { this.platforms = platforms; }

    public List<String> getCompanies() { return companies; }
    public void setCompanies(List<String> companies) { this.companies = companies; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
}
