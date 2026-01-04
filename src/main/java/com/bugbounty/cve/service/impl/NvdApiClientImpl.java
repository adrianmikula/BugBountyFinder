package com.bugbounty.cve.service.impl;

import com.bugbounty.cve.domain.CVE;
import com.bugbounty.cve.service.NvdApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of NVD API client using WebClient for reactive HTTP requests.
 * 
 * NVD API Documentation: https://nvd.nist.gov/developers/vulnerabilities
 * API Base URL: https://services.nvd.nist.gov/rest/json/cves/2.0
 */
@Service
@Slf4j
public class NvdApiClientImpl implements NvdApiClient {
    
    private static final DateTimeFormatter NVD_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    @Value("${app.cve.nvd.api-base-url:https://services.nvd.nist.gov/rest/json}")
    private String nvdApiBaseUrl;
    
    private final Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${app.cve.nvd.api-key:}")
    private String apiKey;
    
    @Value("${app.cve.nvd.rate-limit-delay-ms:6000}")
    private long rateLimitDelayMs;
    
    private WebClient webClient;
    
    public NvdApiClientImpl(
            @org.springframework.beans.factory.annotation.Qualifier("nvdWebClientBuilder") Builder webClientBuilder,
            ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }
    
    @jakarta.annotation.PostConstruct
    public void init() {
        this.webClient = webClientBuilder
                .baseUrl(nvdApiBaseUrl)
                .build();
    }
    
    private WebClient getWebClient() {
        if (webClient == null) {
            init();
        }
        return webClient;
    }
    
    @Override
    public Flux<CVE> fetchRecentCVEs(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching recent CVEs from NVD API: {} to {}", startDate, endDate);
        
        String startDateStr = startDate.format(NVD_DATE_FORMATTER);
        String endDateStr = endDate.format(NVD_DATE_FORMATTER);
        
        return fetchCVEsWithPagination(startDateStr, endDateStr, null)
                .doOnError(error -> log.error("Error fetching recent CVEs from NVD", error))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof org.springframework.web.reactive.function.client.WebClientException));
    }
    
    @Override
    public Flux<CVE> fetchCVEById(String cveId) {
        log.debug("Fetching CVE by ID: {}", cveId);
        
        return getWebClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cves/2.0")
                        .queryParam("cveId", cveId)
                        .build())
                .header("apiKey", apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .flatMapMany(response -> {
                    try {
                        return parseCVEResponse(response);
                    } catch (Exception e) {
                        log.error("Error parsing CVE response for {}", cveId, e);
                        return Flux.empty();
                    }
                })
                .delayElements(Duration.ofMillis(rateLimitDelayMs)) // NVD rate limit: 5 requests per 30 seconds
                .doOnError(error -> log.error("Error fetching CVE {} from NVD", cveId, error));
    }
    
    @Override
    public Flux<CVE> fetchCVEsBySeverity(List<String> severities, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching CVEs by severity: {} from {} to {}", severities, startDate, endDate);
        
        String startDateStr = startDate.format(NVD_DATE_FORMATTER);
        String endDateStr = endDate.format(NVD_DATE_FORMATTER);
        
        // NVD API uses cvssV3Severity parameter
        return Flux.fromIterable(severities)
                .flatMap(severity -> fetchCVEsWithPagination(startDateStr, endDateStr, severity))
                .doOnError(error -> log.error("Error fetching CVEs by severity from NVD", error));
    }
    
    private Flux<CVE> fetchCVEsWithPagination(String startDate, String endDate, String severity) {
        return Flux.defer(() -> {
            int startIndex = 0;
            int resultsPerPage = 2000; // NVD max is 2000
            
            return Flux.generate(
                    () -> startIndex,
                    (currentIndex, sink) -> {
                        try {
                            String response = fetchCVEPage(currentIndex, resultsPerPage, startDate, endDate, severity);
                            JsonNode root = objectMapper.readTree(response);
                            
                            int totalResults = root.path("totalResults").asInt(0);
                            JsonNode vulnerabilities = root.path("vulnerabilities");
                            
                            if (vulnerabilities.isArray() && vulnerabilities.size() > 0) {
                                for (JsonNode vuln : vulnerabilities) {
                                    CVE cve = parseCVEFromNode(vuln);
                                    if (cve != null) {
                                        sink.next(cve);
                                    }
                                }
                            }
                            
                            int nextIndex = currentIndex + resultsPerPage;
                            if (nextIndex >= totalResults) {
                                sink.complete();
                            } else {
                                // Rate limiting: NVD allows 5 requests per 30 seconds
                                try {
                                    Thread.sleep(rateLimitDelayMs);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    sink.complete();
                                }
                                return nextIndex;
                            }
                        } catch (Exception e) {
                            log.error("Error fetching CVE page at index {}", currentIndex, e);
                            sink.error(e);
                        }
                        return currentIndex;
                    }
            );
        });
    }
    
    private String fetchCVEPage(int startIndex, int resultsPerPage, String startDate, String endDate, String severity) {
        WebClient.RequestHeadersSpec<?> request = getWebClient().get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/cves/2.0")
                            .queryParam("pubStartDate", startDate)
                            .queryParam("pubEndDate", endDate)
                            .queryParam("startIndex", startIndex)
                            .queryParam("resultsPerPage", resultsPerPage);
                    
                    if (severity != null && !severity.isEmpty()) {
                        uriBuilder.queryParam("cvssV3Severity", severity);
                    }
                    
                    return uriBuilder.build();
                });
        
        if (apiKey != null && !apiKey.isEmpty()) {
            request = request.header("apiKey", apiKey);
        }
        
        return request.retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));
    }
    
    private Flux<CVE> parseCVEResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode vulnerabilities = root.path("vulnerabilities");
            
            if (!vulnerabilities.isArray()) {
                return Flux.empty();
            }
            
            List<CVE> cves = new ArrayList<>();
            for (JsonNode vuln : vulnerabilities) {
                CVE cve = parseCVEFromNode(vuln);
                if (cve != null) {
                    cves.add(cve);
                }
            }
            
            return Flux.fromIterable(cves);
        } catch (Exception e) {
            log.error("Error parsing CVE response", e);
            return Flux.empty();
        }
    }
    
    private CVE parseCVEFromNode(JsonNode vulnNode) {
        try {
            JsonNode cveNode = vulnNode.path("cve");
            String cveId = cveNode.path("id").asText();
            
            if (cveId == null || cveId.isEmpty()) {
                return null;
            }
            
            // Extract description
            String description = "";
            JsonNode descriptions = cveNode.path("descriptions");
            if (descriptions.isArray() && descriptions.size() > 0) {
                for (JsonNode desc : descriptions) {
                    if ("en".equals(desc.path("lang").asText())) {
                        description = desc.path("value").asText();
                        break;
                    }
                }
            }
            
            // Extract CVSS score and severity
            String severity = "UNKNOWN";
            Double cvssScore = null;
            
            JsonNode metrics = cveNode.path("metrics");
            if (metrics.has("cvssMetricV31")) {
                JsonNode cvssV31 = metrics.path("cvssMetricV31").get(0);
                if (cvssV31 != null) {
                    cvssScore = cvssV31.path("cvssData").path("baseScore").asDouble();
                    severity = mapCvssScoreToSeverity(cvssScore);
                }
            } else if (metrics.has("cvssMetricV30")) {
                JsonNode cvssV30 = metrics.path("cvssMetricV30").get(0);
                if (cvssV30 != null) {
                    cvssScore = cvssV30.path("cvssData").path("baseScore").asDouble();
                    severity = mapCvssScoreToSeverity(cvssScore);
                }
            } else if (metrics.has("cvssMetricV2")) {
                JsonNode cvssV2 = metrics.path("cvssMetricV2").get(0);
                if (cvssV2 != null) {
                    cvssScore = cvssV2.path("cvssData").path("baseScore").asDouble();
                    severity = mapCvssScoreToSeverity(cvssScore);
                }
            }
            
            // Extract published and modified dates
            LocalDateTime publishedDate = parseDate(cveNode.path("published").asText());
            LocalDateTime lastModifiedDate = parseDate(cveNode.path("lastModified").asText());
            
            // Extract affected languages and products from configurations
            List<String> affectedLanguages = extractAffectedLanguages(cveNode);
            List<String> affectedProducts = extractAffectedProducts(cveNode);
            
            return CVE.builder()
                    .cveId(cveId)
                    .description(description)
                    .severity(severity)
                    .cvssScore(cvssScore)
                    .publishedDate(publishedDate)
                    .lastModifiedDate(lastModifiedDate)
                    .affectedLanguages(affectedLanguages)
                    .affectedProducts(affectedProducts)
                    .source("NVD")
                    .build();
        } catch (Exception e) {
            log.error("Error parsing CVE from node", e);
            return null;
        }
    }
    
    private String mapCvssScoreToSeverity(Double score) {
        if (score == null) {
            return "UNKNOWN";
        }
        if (score >= 9.0) {
            return "CRITICAL";
        } else if (score >= 7.0) {
            return "HIGH";
        } else if (score >= 4.0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            // NVD dates are in ISO 8601 format: 2024-01-01T00:00:00.000
            return LocalDateTime.parse(dateStr, NVD_DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr, e);
            return LocalDateTime.now();
        }
    }
    
    private List<String> extractAffectedLanguages(JsonNode cveNode) {
        List<String> languages = new ArrayList<>();
        
        // Try to extract from configurations -> nodes -> cpeMatch
        JsonNode configurations = cveNode.path("configurations");
        if (configurations.isArray()) {
            for (JsonNode config : configurations) {
                JsonNode nodes = config.path("nodes");
                if (nodes.isArray()) {
                    for (JsonNode node : nodes) {
                        JsonNode cpeMatches = node.path("cpeMatch");
                        if (cpeMatches.isArray()) {
                            for (JsonNode cpeMatch : cpeMatches) {
                                String cpe = cpeMatch.path("criteria").asText();
                                // Extract language from CPE string (e.g., cpe:2.3:a:apache:log4j:2.0:*:*:*:*:java:*:*)
                                String lang = extractLanguageFromCPE(cpe);
                                if (lang != null && !languages.contains(lang)) {
                                    languages.add(lang);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return languages;
    }
    
    private List<String> extractAffectedProducts(JsonNode cveNode) {
        List<String> products = new ArrayList<>();
        
        JsonNode configurations = cveNode.path("configurations");
        if (configurations.isArray()) {
            for (JsonNode config : configurations) {
                JsonNode nodes = config.path("nodes");
                if (nodes.isArray()) {
                    for (JsonNode node : nodes) {
                        JsonNode cpeMatches = node.path("cpeMatch");
                        if (cpeMatches.isArray()) {
                            for (JsonNode cpeMatch : cpeMatches) {
                                String cpe = cpeMatch.path("criteria").asText();
                                // Extract product from CPE string
                                String product = extractProductFromCPE(cpe);
                                if (product != null && !products.contains(product)) {
                                    products.add(product);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return products;
    }
    
    private String extractLanguageFromCPE(String cpe) {
        if (cpe == null || !cpe.startsWith("cpe:")) {
            return null;
        }
        
        // CPE format: cpe:2.3:a:vendor:product:version:update:edition:language:sw_edition:target_sw:target_hw:other
        String[] parts = cpe.split(":");
        if (parts.length > 7) {
            String language = parts[7];
            // Map common CPE language codes to programming languages
            return mapCPELanguageToProgrammingLanguage(language);
        }
        return null;
    }
    
    private String extractProductFromCPE(String cpe) {
        if (cpe == null || !cpe.startsWith("cpe:")) {
            return null;
        }
        
        // CPE format: cpe:2.3:a:vendor:product:version:...
        String[] parts = cpe.split(":");
        if (parts.length > 4) {
            String vendor = parts[3];
            String product = parts[4];
            if (vendor != null && product != null && !vendor.equals("*") && !product.equals("*")) {
                return vendor + " " + product;
            }
        }
        return null;
    }
    
    private String mapCPELanguageToProgrammingLanguage(String cpeLanguage) {
        if (cpeLanguage == null || cpeLanguage.equals("*") || cpeLanguage.isEmpty()) {
            return null;
        }
        
        // Map CPE language codes to common programming language names
        return switch (cpeLanguage.toLowerCase()) {
            case "java", "j2ee", "j2se" -> "Java";
            case "python" -> "Python";
            case "javascript", "js", "node" -> "JavaScript";
            case "typescript", "ts" -> "TypeScript";
            case "ruby" -> "Ruby";
            case "php" -> "PHP";
            case "go", "golang" -> "Go";
            case "rust" -> "Rust";
            case "c", "c++", "cpp" -> "C++";
            case "csharp", "c#" -> "C#";
            case "swift" -> "Swift";
            case "kotlin" -> "Kotlin";
            case "scala" -> "Scala";
            default -> null;
        };
    }
}

