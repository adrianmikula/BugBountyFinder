package com.bugbounty.cve.mapper;

import com.bugbounty.cve.domain.CVECatalog;
import com.bugbounty.cve.entity.CVECatalogEntity;
import org.springframework.stereotype.Component;

@Component
public class CVECatalogMapper {
    
    public CVECatalogEntity toEntity(CVECatalog catalog) {
        if (catalog == null) {
            return null;
        }
        
        return CVECatalogEntity.builder()
                .id(catalog.getId())
                .cveId(catalog.getCveId())
                .language(catalog.getLanguage())
                .summary(catalog.getSummary())
                .codeExample(catalog.getCodeExample())
                .vulnerablePattern(catalog.getVulnerablePattern())
                .fixedPattern(catalog.getFixedPattern())
                .createdAt(catalog.getCreatedAt())
                .updatedAt(catalog.getUpdatedAt())
                .build();
    }
    
    public CVECatalog toDomain(CVECatalogEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return CVECatalog.builder()
                .id(entity.getId())
                .cveId(entity.getCveId())
                .language(entity.getLanguage())
                .summary(entity.getSummary())
                .codeExample(entity.getCodeExample())
                .vulnerablePattern(entity.getVulnerablePattern())
                .fixedPattern(entity.getFixedPattern())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

