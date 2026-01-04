package com.bugbounty.cve.repository;

import com.bugbounty.cve.entity.CVECatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CVECatalogRepository extends JpaRepository<CVECatalogEntity, UUID> {
    
    Optional<CVECatalogEntity> findByCveIdAndLanguage(String cveId, String language);
    
    List<CVECatalogEntity> findByCveId(String cveId);
    
    List<CVECatalogEntity> findByLanguage(String language);
    
    List<CVECatalogEntity> findByLanguageIn(List<String> languages);
}

