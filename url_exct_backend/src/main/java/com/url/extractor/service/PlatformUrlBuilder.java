package com.url.extractor.service;

import com.url.extractor.dto.JobSearchFilter;
import jakarta.inject.Singleton;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Singleton
public class PlatformUrlBuilder {

    @jakarta.inject.Inject
    private GroqService groqService;

    public String buildUrl(JobSearchFilter filter, String platform) {
        String staticUrl = buildStaticUrl(filter, platform);
        if (groqService != null && groqService.hasApiKey()) {
            return groqService.generatePlatformSearchUrl(platform, filter, staticUrl);
        }
        return staticUrl;
    }

    private String buildStaticUrl(JobSearchFilter filter, String platform) {
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
        // Shine uses Next.js slug-based routing — ?q= param is IGNORED by the router.
        // Correct format: /job-search/java-developer-jobs or /job-search/java-developer-jobs-in-mumbai
        String optimized = getOptimizedQuery(filter, 3);
        String slug = optimized.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");

        String location = "";
        if (filter.getLocations() != null && !filter.getLocations().isEmpty()) {
            location = filter.getLocations().get(0);
        } else if (filter.getCountry() != null && !filter.getCountry().isEmpty()) {
            String country = getFullCountryName(filter.getCountry());
            if (!country.equalsIgnoreCase("India")) {
                location = country;
            }
        }

        StringBuilder url = new StringBuilder("https://www.shine.com/job-search/").append(slug).append("-jobs");
        if (!location.isEmpty()) {
            String locSlug = location.toLowerCase().trim().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-");
            url.append("-in-").append(locSlug);
        }
        // Keep ?sort=1 for freshness and add q= as secondary signal (some versions use it)
        url.append("?sort=1&q=").append(URLEncoder.encode(optimized, StandardCharsets.UTF_8));
        return url.toString();
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
        
        if (filter.getDatePosted() != null && !filter.getDatePosted().isBlank()) {
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

        if (filter.getDatePosted() != null && !filter.getDatePosted().isBlank()) {
            url.append("&jobFreshness=").append(filter.getDatePosted());
        }
        
        if (filter.getPageAsInt() != null && filter.getPageAsInt() > 1) {
            url.append("&pageNo=").append(filter.getPageAsInt());
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
        if (filter.getPageAsInt() != null && filter.getPageAsInt() > 1) {
            url += "/page-" + filter.getPageAsInt();
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

        if (filter.getDatePosted() != null && !filter.getDatePosted().isBlank()) {
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

        if (filter.getPageAsInt() != null && filter.getPageAsInt() > 1) {
            url.append(url.toString().contains("?") ? "&" : "?").append("page=").append(filter.getPageAsInt());
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

        if (filter.getDatePosted() != null && !filter.getDatePosted().isBlank()) {
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
        String country = filter.getCountry() != null ? filter.getCountry().toLowerCase() : "in";

        // LinkedIn India uses a slug-based URL: in.linkedin.com/jobs/{slug}-jobs
        // General LinkedIn uses: linkedin.com/jobs/search?keywords=...
        String slug = query.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");

        // Use country-specific subdomain
        String baseUrl;
        if (country.equals("in")) {
            baseUrl = "https://in.linkedin.com/jobs/" + slug + "-jobs";
        } else if (country.equals("gb") || country.equals("uk")) {
            baseUrl = "https://uk.linkedin.com/jobs/" + slug + "-jobs";
        } else if (country.equals("us")) {
            baseUrl = "https://www.linkedin.com/jobs/" + slug + "-jobs-united-states";
        } else {
            baseUrl = "https://www.linkedin.com/jobs/search?keywords=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        }

        StringBuilder url = new StringBuilder(baseUrl);

        List<String> locations = filter.getLocations();
        if (locations != null && !locations.isEmpty()) {
            // For slug-based URLs, append location; for search URLs, use &location=
            if (baseUrl.contains("?")) {
                url.append("&location=").append(URLEncoder.encode(locations.get(0), StandardCharsets.UTF_8));
            } else {
                url.append("?location=").append(URLEncoder.encode(locations.get(0), StandardCharsets.UTF_8));
            }
        }

        // Add pagination
        if (filter.getPageAsInt() != null && filter.getPageAsInt() > 1) {
            String sep = url.toString().contains("?") ? "&" : "?";
            url.append(sep).append("position=1&pageNum=").append(filter.getPageAsInt() - 1);
        } else {
            String sep = url.toString().contains("?") ? "&" : "?";
            url.append(sep).append("position=1&pageNum=0");
        }

        // Date filter (LinkedIn uses f_TPR in seconds)
        if (filter.getDatePostedAsInt() != null) {
            long seconds = filter.getDatePostedAsInt() * 86400L;
            url.append("&f_TPR=r").append(seconds);
        }

        // Experience filter
        if (filter.getExperienceLevel() != null) {
            String exp = filter.getExperienceLevel().toLowerCase();
            if (exp.contains("intern")) url.append("&f_E=1");
            else if (exp.contains("entry")) url.append("&f_E=2");
            else if (exp.contains("mid")) url.append("&f_E=3");
            else if (exp.contains("senior")) url.append("&f_E=4");
        }

        // Work mode filter
        if (filter.getWorkMode() != null) {
            String mode = filter.getWorkMode().toLowerCase();
            if (mode.contains("onsite")) url.append("&f_WT=1");
            else if (mode.contains("remote")) url.append("&f_WT=2");
            else if (mode.contains("hybrid")) url.append("&f_WT=3");
        }

        // Job type filter
        if (filter.getJobType() != null) {
            String jt = filter.getJobType().toLowerCase();
            if (jt.contains("full")) url.append("&f_JT=F");
            else if (jt.contains("contract")) url.append("&f_JT=C");
            else if (jt.contains("part")) url.append("&f_JT=P");
            else if (jt.contains("intern")) url.append("&f_JT=I");
        }

        return url.toString();
    }


    private String buildIndeedUrl(JobSearchFilter filter) {
        String optimizedQuery = getOptimizedQuery(filter, 3);
        StringBuilder query = new StringBuilder(optimizedQuery);

        boolean hasComplexFilters = false;
        if (filter.getAdditionalKeywords() != null && !filter.getAdditionalKeywords().isEmpty()) {
            query.append(" ").append(filter.getAdditionalKeywords());
        }

        if (filter.getCompanies() != null && !filter.getCompanies().isEmpty()) {
            hasComplexFilters = true;
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

        List<String> locations = filter.getLocations();
        String primaryLocation = "";
        if (locations != null && !locations.isEmpty()) {
            primaryLocation = locations.get(0);
        }

        String countryIso = filter.getCountry() != null ? filter.getCountry().toLowerCase() : "in";
        boolean isIndia = "in".equals(countryIso);

        // For Indeed India without complex company boolean queries, use the direct SEO slug URL:
        // https://in.indeed.com/q-java-developer-jobs.html (as confirmed by user)
        if (isIndia && !hasComplexFilters) {
            String slug = optimizedQuery.toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", "")
                    .trim()
                    .replaceAll("\\s+", "-");

            StringBuilder slugUrl = new StringBuilder("https://in.indeed.com/q-").append(slug);

            if (!primaryLocation.isEmpty() && !"india".equalsIgnoreCase(primaryLocation)) {
                String locSlug = primaryLocation.toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", "")
                        .trim()
                        .replaceAll("\\s+", "-");
                slugUrl.append("-l-").append(locSlug);
            }
            slugUrl.append("-jobs.html");

            // Append filter parameters if any
            StringBuilder params = new StringBuilder();
            if (filter.getDatePosted() != null && !filter.getDatePosted().isBlank()) {
                params.append("fromage=").append(filter.getDatePosted());
            }
            if (filter.getExperienceLevel() != null && !filter.getExperienceLevel().isEmpty()) {
                if (params.length() > 0) params.append("&");
                params.append("explvl=").append(filter.getExperienceLevel());
            }
            if (filter.getJobType() != null && !filter.getJobType().isEmpty()) {
                if (params.length() > 0) params.append("&");
                params.append("jt=").append(filter.getJobType().toLowerCase());
            }
            if (filter.getPageAsInt() != null && filter.getPageAsInt() > 1) {
                if (params.length() > 0) params.append("&");
                params.append("start=").append((filter.getPageAsInt() - 1) * 10);
            }

            if (params.length() > 0) {
                slugUrl.append("?").append(params);
            }

            return slugUrl.toString();
        }

        // Standard /jobs?q= fallback for international or complex queries
        String baseUrl = isIndia ? "https://in.indeed.com/jobs" : "https://www.indeed.com/jobs";
        if (!isIndia && filter.getCountry() != null) {
            switch (countryIso) {
                case "gb": case "uk": baseUrl = "https://uk.indeed.com/jobs"; break;
                case "ca": baseUrl = "https://ca.indeed.com/jobs"; break;
                case "au": baseUrl = "https://au.indeed.com/jobs"; break;
                case "us": baseUrl = "https://www.indeed.com/jobs"; break;
                default: baseUrl = "https://" + countryIso + ".indeed.com/jobs"; break;
            }
        }

        String encodedQuery = URLEncoder.encode(query.toString().trim(), StandardCharsets.UTF_8);
        StringBuilder url = new StringBuilder(baseUrl).append("?q=").append(encodedQuery);

        if (!primaryLocation.trim().isEmpty() && !("India".equalsIgnoreCase(primaryLocation) && isIndia)) {
            url.append("&l=").append(URLEncoder.encode(primaryLocation, StandardCharsets.UTF_8));
        }
        if (filter.getJobType() != null && !filter.getJobType().isEmpty()) {
            url.append("&jt=").append(filter.getJobType().toLowerCase());
        }
        if (filter.getDatePosted() != null && !filter.getDatePosted().isBlank()) {
            url.append("&fromage=").append(filter.getDatePosted());
        }
        if (filter.getExperienceLevel() != null && !filter.getExperienceLevel().isEmpty()) {
            url.append("&explvl=").append(filter.getExperienceLevel());
        }
        if (filter.getPageAsInt() != null && filter.getPageAsInt() > 1) {
            int start = (filter.getPageAsInt() - 1) * 10;
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
