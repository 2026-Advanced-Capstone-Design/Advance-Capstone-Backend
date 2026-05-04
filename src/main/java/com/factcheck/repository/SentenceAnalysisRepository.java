package com.factcheck.repository;

import com.factcheck.domain.SentenceAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SentenceAnalysisRepository extends JpaRepository<SentenceAnalysis, Long> {
}
