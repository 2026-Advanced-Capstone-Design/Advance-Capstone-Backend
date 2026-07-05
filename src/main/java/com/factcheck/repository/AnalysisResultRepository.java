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
           "WHERE r.article.id = :articleId")
    Optional<AnalysisResult> findByArticleId(@Param("articleId") Long articleId);

    Optional<AnalysisResult> findTopByOrderByIdDesc();

    // 멱등성 가드용: 해당 기사의 분석 결과가 이미 저장돼 있는지 확인(중복 콜백 조기 차단).
    @Query("SELECT COUNT(r) > 0 FROM AnalysisResult r WHERE r.article.id = :articleId")
    boolean existsByArticleId(@Param("articleId") Long articleId);
}
