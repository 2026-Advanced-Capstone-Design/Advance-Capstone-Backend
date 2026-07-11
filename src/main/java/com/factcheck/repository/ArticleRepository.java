package com.factcheck.repository;

import com.factcheck.domain.Article;
import com.factcheck.Enum.ArticleStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    // 조건부 상태 전이(CAS): 현재 상태가 expectedStatus일 때만 newStatus로 변경한다.
    // 반환값 = 실제로 갱신된 행 수(0이면 이미 다른 상태여서 전이하지 않음).
    // 늦게 커밋된 쓰기가 앞선 전이(예: 콜백의 DONE)를 덮어쓰는 경합(A4)을 DB 원자성으로 차단.
    @Modifying
    @Query("UPDATE Article a SET a.status = :newStatus " +
           "WHERE a.id = :id AND a.status = :expectedStatus")
    int updateStatusIfCurrent(@Param("id") Long id,
                              @Param("expectedStatus") ArticleStatus expectedStatus,
                              @Param("newStatus") ArticleStatus newStatus);

    // 재조정 스위퍼용: 임계시각(threshold) 이전에 생성됐는데 아직 미완료 상태(PENDING/ANALYZING)에
    // 머물러 있는 기사 = 콜백 유실 등으로 stuck된 후보. 오래된 것부터, 한 번에 최대 Pageable 개수만.
    @Query("SELECT a FROM Article a " +
           "WHERE a.status IN (:statuses) AND a.createdAt < :threshold " +
           "ORDER BY a.createdAt ASC")
    List<Article> findStuck(@Param("statuses") Collection<ArticleStatus> statuses,
                            @Param("threshold") LocalDateTime threshold,
                            Pageable pageable);
}
