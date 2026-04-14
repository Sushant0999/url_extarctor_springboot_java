package com.url.extractor.service;

import com.url.extractor.dto.JobSearchFilter;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class PlatformUrlBuilder {

    public String buildUrl(JobSearchFilter filter, String platform) {
        String p = platform.toLowerCase();
        if (p.contains("linkedin")) return buildLinkedInUrl(filter);
        if (p.contains("naukri")) return buildNaukriUrl(filter);
        if (p.contains("cutshort")) return buildCutshortUrl(filter);
        if (p.contains("foundit")) return buildFounditUrl(filter);
        if (p.contains("internshala")) return buildInternshalaUrl(filter);
        if (p.contains("shine")) return buildShineUrl(filter);
        if (p.contains("hirist")) return buildHiristUrl(filter);
        return buildIndeedUrl(filter);
    }
    
    private String buildShineUrl(JobSearchFilter filter) {
        String optimized = getOptimizedQuery(filter, 3);
        String url = "https://www.shine.com/job-search/jobs?q=" + URLEncoder.encode(optimized, StandardCharsets.UTF_8);
        String location = "";
        if (filter.getLocations() != null && !filter.getLocations().isEmpty()) {
            location = filter.getLocations().get(0);
        } else if (filter.getCountry() != null && !filter.getCountry().isEmpty()) {
            location = getFullCountryName(filter.getCountry());
        }
        if (!location.isEmpty()) {
            url += "&loc=" + URLEncoder.encode(location, StandardCharsets.UTF_8);
        }
        url += "&sort=1"; // Sort by freshness
        return url;
    }

    private String buildHiristUrl(JobSearchFilter filter) {
        String query = getOptimizedQuery(filter, 3);
        String encodedQuery = URLEncoder.encode(query.toLowerCase().replace(" ", "-"), StandardCharsets.UTF_8);
        StringBuilder url = new StringBuilder("https://www.hirist.tech/search/").append(encodedQuery).append("-jobs");
        
        String location = "";
        if (filter.getLocations() != null && !filter.getLocations().isEmpty()) {
            location = filter.getLocations().get(0);
        }
        
        if (!location.isEmpty()) {
            url.append("?loc=").append(URLEncoder.encode(location, StandardCharsets.UTF_8));
        }
        
        if (filter.getDatePosted() != null) {
            url.append(url.toString().contains("?") ? "&" : "?").append("posting=").append(filter.getDatePosted());
        }
        
        return url.toString();
    }

    private String buildFounditUrl(JobSearchFilter filter) {
        String query = getOptimizedQuery(filter, 3);
        StringBuilder url = new StringBuilder("https://www.foundit.in/srp/results?query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
        
        if (filter.getLocations() != null && !filter.getLocations().isEmpty()) {
            url.append("&locations=").append(URLEncoder.encode(filter.getLocations().get(0), StandardCharsets.UTF_8));
        }

        if (filter.getDatePosted() != null) {
            url.append("&jobFreshness=").append(filter.getDatePosted());
        }
        
        if (filter.getPage() != null && filter.getPage() > 1) {
            url.append("&pageNo=").append(filter.getPage());
        }
        return url.toString();
    }

    private String buildInternshalaUrl(JobSearchFilter filter) {
        String query = filter.getQuery() != null ? filter.getQuery() : "";
        if (filter.getSkills() != null && !filter.getSkills().isEmpty()) {
            query += " " + String.join(" ", filter.getSkills());
        }
        String encodedQuery = URLEncoder.encode(query.trim().replace(" ", "-"), StandardCharsets.UTF_8);
        String location = "";
        if (filter.getLocations() != null && !filter.getLocations().isEmpty()) {
            location = "/location-" + URLEncoder.encode(filter.getLocations().get(0).toLowerCase().replace(" ", "-"), StandardCharsets.UTF_8);
        } else if (filter.getCountry() != null && !filter.getCountry().isEmpty()) {
            String country = getFullCountryName(filter.getCountry()).toLowerCase().replace(" ", "-");
            if (!country.equals("india")) {
                location = "/location-" + URLEncoder.encode(country, StandardCharsets.UTF_8);
            }
        }
        
        String url = "https://internshala.com/internships/keywords-" + encodedQuery.toLowerCase() + location;
        if (filter.getPage() != null && filter.getPage() > 1) {
            url += "/page-" + filter.getPage();
        }
        return url;
    }

    private String buildNaukriUrl(JobSearchFilter filter) {
        String query = filter.getQuery() != null ? filter.getQuery() : "";
        if (filter.getSkills() != null && !filter.getSkills().isEmpty()) {
            query += " " + String.join(" ", filter.getSkills());
        }
        
        String encodedQuery = URLEncoder.encode(query.trim().replace(" ", "-"), StandardCharsets.UTF_8);
        String location = "";
        if (filter.getLocations() != null && !filter.getLocations().isEmpty()) {
            location = filter.getLocations().get(0).toLowerCase().trim().replace(" ", "-");
        } else if (filter.getCountry() != null && !filter.getCountry().isEmpty()) {
            String country = getFullCountryName(filter.getCountry());
            if (!country.equalsIgnoreCase("India")) {
                location = country.toLowerCase().trim().replace(" ", "-");
            }
        }

        // Naukri semantic URL: naukri.com/java-developer-jobs or ...-jobs-in-bangalore
        String optimized = getOptimizedQuery(filter, 3);
        StringBuilder url = new StringBuilder("https://www.naukri.com/")
                .append(URLEncoder.encode(optimized.toLowerCase().replace(" ", "-"), StandardCharsets.UTF_8))
                .append("-jobs");
                
        if (!location.isEmpty() && location.length() < 50) {
            url.append("-in-").append(URLEncoder.encode(location.toLowerCase().replace(" ", "-"), StandardCharsets.UTF_8));
        }

        // Add robust query parameters
        url.append("?k=").append(URLEncoder.encode(optimized, StandardCharsets.UTF_8));
        if (!location.isEmpty()) url.append("&l=").append(URLEncoder.encode(location, StandardCharsets.UTF_8));

        if (filter.getDatePosted() != null) {
            url.append("&jobAge=").append(filter.getDatePosted());
        }

        // Handle Experience (?experience=3)
        if (filter.getExperienceLevel() != null && !filter.getExperienceLevel().isEmpty()) {
            String exp = filter.getExperienceLevel().toLowerCase();
            String years = null;
            
            if (exp.matches("\\d+")) {
                years = exp; // If user entered "3"
            } else if (exp.contains("entry")) {
                years = "0";
            } else if (exp.contains("mid")) {
                years = "3";
            } else if (exp.contains("senior")) {
                years = "7";
            }
            
            if (years != null) {
                url.append("?experience=").append(years);
            }
        }

        if (filter.getPage() != null && filter.getPage() > 1) {
            url.append(url.toString().contains("?") ? "&" : "?").append("page=").append(filter.getPage());
        }

        return url.toString();
    }

    private String buildCutshortUrl(JobSearchFilter filter) {
        String query = getOptimizedQuery(filter, 3);
        StringBuilder url = new StringBuilder("https://cutshort.io/search-jobs")
                .append("?free_text=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
        
        if (filter.getLocations() != null && !filter.getLocations().isEmpty()) {
            url.append("&locations=").append(URLEncoder.encode(filter.getLocations().get(0), StandardCharsets.UTF_8));
        }

        if (filter.getDatePosted() != null) {
            url.append("&posted_within=").append(filter.getDatePosted());
        }

        return url.toString();
    }

    private String getFullCountryName(String iso) {
        if (iso == null) return "India";
        switch (iso.toLowerCase()) {
            case "in": return "India";
            case "us": return "United States";
            case "gb": case "uk": return "United Kingdom";
            case "ca": return "Canada";
            case "au": return "Australia";
            case "de": return "Germany";
            case "fr": return "France";
            default: return iso;
        }
    }

    private String buildLinkedInUrl(JobSearchFilter filter) {
        String query = getOptimizedQuery(filter, 3);

        String baseUrl = "https://www.linkedin.com/jobs/search";
        StringBuilder url = new StringBuilder(baseUrl)
                .append("?keywords=")
                .append(URLEncoder.encode(query, StandardCharsets.UTF_8));

        List<String> locations = filter.getLocations();
        if (locations != null && !locations.isEmpty()) {
            url.append("&location=").append(URLEncoder.encode(locations.get(0), StandardCharsets.UTF_8));
        } else if ("remote".equalsIgnoreCase(filter.getWorkMode())) {
            url.append("&location=Remote");
        } else {
            // Default to full country name to avoid ISO collisions (e.g., IN -> India, not Indiana)
            url.append("&location=").append(URLEncoder.encode(getFullCountryName(filter.getCountry()), StandardCharsets.UTF_8));
        }

        if (filter.getDatePosted() != null) {
            long seconds = filter.getDatePosted() * 86400L;
            url.append("&f_TPR=r").append(seconds);
        }

        if (filter.getExperienceLevel() != null) {
            String exp = filter.getExperienceLevel().toLowerCase();
            if (exp.contains("intern")) url.append("&f_E=1");
            else if (exp.contains("entry")) url.append("&f_E=2");
            else if (exp.contains("mid")) url.append("&f_E=3");
            else if (exp.contains("senior")) url.append("&f_E=4");
        }

        if (filter.getWorkMode() != null) {
            String mode = filter.getWorkMode().toLowerCase();
            if (mode.contains("onsite")) url.append("&f_WT=1");
            else if (mode.contains("remote")) url.append("&f_WT=2");
            else if (mode.contains("hybrid")) url.append("&f_WT=3");
        }

        if (filter.getJobType() != null) {
            String jt = filter.getJobType().toLowerCase();
            if (jt.contains("full")) url.append("&f_JT=F");
            else if (jt.contains("contract")) url.append("&f_JT=C");
            else if (jt.contains("part")) url.append("&f_JT=P");
            else if (jt.contains("intern")) url.append("&f_JT=I");
        }

        if (filter.getDistance() != null) {
            url.append("&distance=").append(filter.getDistance());
        }

        if (filter.getPage() != null && filter.getPage() > 1) {
            int start = (filter.getPage() - 1) * 25;
            url.append("&start=").append(start);
        }

        return url.toString();
    }

    private String buildIndeedUrl(JobSearchFilter filter) {
        String optimizedQuery = getOptimizedQuery(filter, 5);
        StringBuilder query = new StringBuilder(optimizedQuery);

        if (filter.getAdditionalKeywords() != null && !filter.getAdditionalKeywords().isEmpty()) {
            query.append(" ").append(filter.getAdditionalKeywords());
        }

        if (filter.getCompanies() != null && !filter.getCompanies().isEmpty()) {
            query.append(" (");
            for (int i = 0; i < filter.getCompanies().size(); i++) {
                query.append("company:\"").append(filter.getCompanies().get(i)).append("\"");
                if (i < filter.getCompanies().size() - 1) {
                    query.append(" OR ");
                }
            }
            query.append(")");
        }

        if (filter.getWorkMode() != null && !filter.getWorkMode().isEmpty()) {
            String mode = filter.getWorkMode().toLowerCase();
            if (mode.equals("hybrid")) query.append(" hybrid");
            else if (mode.equals("onsite")) query.append(" \"on-site\" OR \"work from office\"");
            if (mode.equals("remote")) query.append(" remote");
        }

        String encodedQuery = URLEncoder.encode(query.toString().trim(), StandardCharsets.UTF_8);
        List<String> locations = filter.getLocations();
        String primaryLocation = "";
        
        if (locations != null && !locations.isEmpty()) {
            if (locations.size() > 1) {
                StringBuilder locQuery = new StringBuilder(" (");
                locQuery.append(String.join(" OR ", locations));
                locQuery.append(")");
                encodedQuery += URLEncoder.encode(locQuery.toString(), StandardCharsets.UTF_8);
            } else {
                primaryLocation = locations.get(0);
            }
        }

        if ("remote".equalsIgnoreCase(filter.getWorkMode()) && primaryLocation.isEmpty()) {
            primaryLocation = "Remote";
        } else if (primaryLocation.isEmpty()) {
            // Fix: Pin search to full country name instead of ISO code (IN -> Indiana collision)
            primaryLocation = getFullCountryName(filter.getCountry());
        }
        
        String baseUrl = "https://in.indeed.com/jobs";
        if (filter.getCountry() != null && !filter.getCountry().isEmpty()) {
            String iso = filter.getCountry().toLowerCase();
            switch (iso) {
                case "in": baseUrl = "https://in.indeed.com/jobs"; break;
                case "gb": case "uk": baseUrl = "https://uk.indeed.com/jobs"; break;
                case "ca": baseUrl = "https://ca.indeed.com/jobs"; break;
                case "au": baseUrl = "https://au.indeed.com/jobs"; break;
                case "us": baseUrl = "https://www.indeed.com/jobs"; break;
                default: baseUrl = "https://" + iso + ".indeed.com/jobs"; break;
            }
        }

        StringBuilder url = new StringBuilder(baseUrl).append("?q=").append(encodedQuery);
        if (!primaryLocation.trim().isEmpty()) {
            url.append("&l=").append(URLEncoder.encode(primaryLocation, StandardCharsets.UTF_8));
        }
        if (filter.getJobType() != null && !filter.getJobType().isEmpty()) {
            url.append("&jt=").append(filter.getJobType().toLowerCase());
        }
        if (filter.getDatePosted() != null) {
            url.append("&fromage=").append(filter.getDatePosted());
        }
        if (filter.getExperienceLevel() != null && !filter.getExperienceLevel().isEmpty()) {
            url.append("&explvl=").append(filter.getExperienceLevel());
        }

        if (filter.getPage() != null && filter.getPage() > 1) {
            int start = (filter.getPage() - 1) * 10;
            url.append("&start=").append(start);
        }

        return url.toString();
    }

    private String getOptimizedQuery(JobSearchFilter filter, int skillLimit) {
        StringBuilder sb = new StringBuilder(filter.getQuery() != null ? filter.getQuery().trim() : "Software Engineer");
        
        if (filter.getSkills() != null && !filter.getSkills().isEmpty()) {
            List<String> uniqueSkills = filter.getSkills().stream()
                    .filter(s -> !sb.toString().toLowerCase().contains(s.toLowerCase()))
                    .limit(skillLimit)
                    .toList();
            
            if (!uniqueSkills.isEmpty()) {
                sb.append(" ").append(String.join(" ", uniqueSkills));
            }
        }
        
        // Remove excessive special characters that break semantic URLs
        return sb.toString().replaceAll("[^a-zA-Z0-9\\s+#\\.]", "").replaceAll("\\s+", " ").trim();
    }
}
