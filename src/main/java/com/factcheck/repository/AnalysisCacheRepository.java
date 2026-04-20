package com.factcheck.repository;

import com.factcheck.domain.AnalysisCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisCacheRepository extends JpaRepository<AnalysisCache, Long> {
    Optional<AnalysisCache> findByUrlHash(String urlHash);
}
