package com.bugbounty.cve.mapper;

import com.bugbounty.cve.domain.CVE;
import com.bugbounty.cve.entity.CVEEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CVEMapper {
    
    private final ObjectMapper objectMapper;
    
    public CVEEntity toEntity(CVE cve) {
        if (cve == null) {
            return null;
        }
        
        try {
            return CVEEntity.builder()
                    .id(cve.getId())
                    .cveId(cve.getCveId())
                    .description(cve.getDescription())
                    .severity(cve.getSeverity())
                    .cvssScore(cve.getCvssScore())
                    .publishedDate(cve.getPublishedDate())
                    .lastModifiedDate(cve.getLastModifiedDate())
                    .affectedLanguages(listToJson(cve.getAffectedLanguages()))
                    .affectedProducts(listToJson(cve.getAffectedProducts()))
                    .source(cve.getSource())
                    .build();
        } catch (Exception e) {
            log.error("Error mapping CVE to entity: {}", cve.getCveId(), e);
            throw new RuntimeException("Failed to map CVE to entity", e);
        }
    }
    
    public CVE toDomain(CVEEntity entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            return CVE.builder()
                    .id(entity.getId())
                    .cveId(entity.getCveId())
                    .description(entity.getDescription())
                    .severity(entity.getSeverity())
                    .cvssScore(entity.getCvssScore())
                    .publishedDate(entity.getPublishedDate())
                    .lastModifiedDate(entity.getLastModifiedDate())
                    .affectedLanguages(jsonToList(entity.getAffectedLanguages()))
                    .affectedProducts(jsonToList(entity.getAffectedProducts()))
                    .source(entity.getSource())
                    .build();
        } catch (Exception e) {
            log.error("Error mapping entity to CVE: {}", entity.getCveId(), e);
            throw new RuntimeException("Failed to map entity to CVE", e);
        }
    }
    
    private String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Error converting list to JSON", e);
            return "[]";
        }
    }
    
    private List<String> jsonToList(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to list: {}", json, e);
            return new ArrayList<>();
        }
    }
}

