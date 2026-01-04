package com.bugbounty.cve.webhook.controller;

import com.bugbounty.cve.domain.CVE;
import com.bugbounty.cve.service.CVEMonitoringService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for handling CVE webhook notifications from external services.
 * Supports webhooks from services like anyCVE, TrackCVE, CVEWatch, etc.
 */
@RestController
@RequestMapping("/api/webhooks/cve")
@RequiredArgsConstructor
@Slf4j
public class CVEWebhookController {
    
    private final CVEMonitoringService cveMonitoringService;
    private final ObjectMapper objectMapper;
    
    /**
     * Handle generic CVE webhook notifications.
     * Accepts JSON payloads from various CVE monitoring services.
     * 
     * Expected payload format:
     * {
     *   "cveId": "CVE-2024-1234",
     *   "description": "...",
     *   "severity": "CRITICAL",
     *   "cvssScore": 9.8,
     *   "publishedDate": "2024-01-01T00:00:00",
     *   "affectedLanguages": ["Java", "Python"],
     *   "affectedProducts": ["Spring Framework", "Apache Log4j"]
     * }
     */
    @PostMapping
    public ResponseEntity<String> handleCVEWebhook(@RequestBody String payload) {
        log.info("Received CVE webhook notification");
        
        try {
            JsonNode root = objectMapper.readTree(payload);
            CVE cve = parseCVEFromWebhook(root);
            
            if (cve == null || cve.getCveId() == null || cve.getCveId().isEmpty()) {
                log.warn("Invalid CVE webhook payload: missing cveId");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid payload: missing cveId");
            }
            
            log.info("Processing CVE webhook for: {}", cve.getCveId());
            cveMonitoringService.handleCVEWebhook(cve);
            
            return ResponseEntity.ok("CVE webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing CVE webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }
    
    /**
     * Health check endpoint for webhook configuration.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("CVE webhook endpoint is active");
    }
    
    /**
     * Parse CVE from webhook JSON payload.
     * Supports multiple webhook formats.
     */
    private CVE parseCVEFromWebhook(JsonNode root) {
        try {
            String cveId = getStringValue(root, "cveId", "cve_id", "id");
            if (cveId == null || cveId.isEmpty()) {
                return null;
            }
            
            String description = getStringValue(root, "description", "summary");
            String severity = getStringValue(root, "severity", "cvss_severity");
            Double cvssScore = getDoubleValue(root, "cvssScore", "cvss_score", "score");
            
            LocalDateTime publishedDate = parseDate(getStringValue(root, "publishedDate", "published_date", "published"));
            LocalDateTime lastModifiedDate = parseDate(getStringValue(root, "lastModifiedDate", "last_modified_date", "lastModified"));
            
            List<String> affectedLanguages = extractList(root, "affectedLanguages", "affected_languages", "languages");
            List<String> affectedProducts = extractList(root, "affectedProducts", "affected_products", "products");
            
            return CVE.builder()
                    .cveId(cveId)
                    .description(description)
                    .severity(severity != null ? severity.toUpperCase() : "UNKNOWN")
                    .cvssScore(cvssScore)
                    .publishedDate(publishedDate != null ? publishedDate : LocalDateTime.now())
                    .lastModifiedDate(lastModifiedDate)
                    .affectedLanguages(affectedLanguages)
                    .affectedProducts(affectedProducts)
                    .source("WEBHOOK")
                    .build();
        } catch (Exception e) {
            log.error("Error parsing CVE from webhook payload", e);
            return null;
        }
    }
    
    private String getStringValue(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key)) {
                JsonNode value = node.get(key);
                if (value.isTextual()) {
                    return value.asText();
                } else if (value.isNumber()) {
                    return value.asText();
                }
            }
        }
        return null;
    }
    
    private Double getDoubleValue(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key)) {
                JsonNode value = node.get(key);
                if (value.isNumber()) {
                    return value.asDouble();
                } else if (value.isTextual()) {
                    try {
                        return Double.parseDouble(value.asText());
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            }
        }
        return null;
    }
    
    private List<String> extractList(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key)) {
                JsonNode array = node.get(key);
                if (array.isArray()) {
                    List<String> list = new ArrayList<>();
                    for (JsonNode item : array) {
                        if (item.isTextual()) {
                            list.add(item.asText());
                        }
                    }
                    return list;
                }
            }
        }
        return new ArrayList<>();
    }
    
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            // Try ISO 8601 format
            return LocalDateTime.parse(dateStr.replace("Z", ""));
        } catch (Exception e) {
            log.debug("Failed to parse date: {}", dateStr);
            return null;
        }
    }
}

