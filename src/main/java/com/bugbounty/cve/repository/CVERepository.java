package com.bugbounty.cve.repository;

import com.bugbounty.cve.entity.CVEEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CVERepository extends JpaRepository<CVEEntity, UUID> {
    
    boolean existsByCveId(String cveId);
    
    Optional<CVEEntity> findByCveId(String cveId);
    
    List<CVEEntity> findBySeverityIn(List<String> severities);
    
    List<CVEEntity> findByPublishedDateAfter(LocalDateTime date);
    
    List<CVEEntity> findBySource(String source);
}

