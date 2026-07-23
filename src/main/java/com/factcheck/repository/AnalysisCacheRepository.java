package com.factcheck.repository;

import com.factcheck.domain.AnalysisCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AnalysisCacheRepository extends JpaRepository<AnalysisCache, Long> {
    Optional<AnalysisCache> findByUrlHash(String urlHash);

    /**
      만료된 캐시 행 벌크 삭제. 조회 경로는 isExpired()로 만료를 걸러내지만
      행 자체는 아무도 지우지 않아 무한 증가하므로 스케줄러가 주기 삭제
     **/
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AnalysisCache c WHERE c.expiresAt < :threshold")
    int deleteExpired(@Param("threshold") LocalDateTime threshold);
}
