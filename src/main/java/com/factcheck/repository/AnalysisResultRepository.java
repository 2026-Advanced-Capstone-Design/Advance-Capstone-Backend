package com.factcheck.repository;

import com.factcheck.domain.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    @Query("SELECT r FROM AnalysisResult r " +
           "JOIN FETCH r.article " +
           "LEFT JOIN FETCH r.sections " +
           "LEFT JOIN FETCH r.sentences " +
           "WHERE r.article.id = :articleId")
    Optional<AnalysisResult> findByArticleId(@Param("articleId") Long articleId);
}
