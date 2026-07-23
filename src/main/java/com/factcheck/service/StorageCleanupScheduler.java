package com.factcheck.service;

import com.factcheck.repository.AnalysisCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

/**
 * 저장 공간 정리 스케줄러 (Phase 3 — 메모리/저장소 무한 증가 차단).
 *
 * <p>AnalysisCache는 {@code expires_at}(7일)이 있고 조회 경로가 {@code isExpired()}로
 * 만료를 걸러내지만, <b>행을 지우는 주체가 없어</b> 테이블이 무한히 커진다.
 * t2.micro 동거 MySQL에서 테이블 비대는 버퍼풀 압박 → 전체 성능 저하로 이어지므로
 * 만료 행을 주기적으로 벌크 삭제한다.
 *
 * <p>새벽 시간 cron 기본값 — 삭제는 수 ms짜리 벌크 DELETE 하나라 부하 영향은 거의 없지만,
 * 습관적으로 트래픽이 가장 적은 시간대에 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageCleanupScheduler {

    private final AnalysisCacheRepository analysisCacheRepository;

    /** 업로드 이미지 보관 일수. OCR로 텍스트 추출이 끝나면 원본 이미지는 분석에 더 쓰이지 않는다. */
    @Value("${storage.cleanup.upload-retention-days:7}")
    private long uploadRetentionDays;

    @Value("${storage.cleanup.upload-dir:uploads/images}")
    private String uploadDir;

    /** 만료된 analysis_cache 행 삭제 (기본: 매일 04:30). */
    @Scheduled(cron = "${storage.cleanup.cache-cron:0 30 4 * * *}")
    @Transactional
    public void purgeExpiredCache() {
        int deleted = analysisCacheRepository.deleteExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("만료 캐시 정리: analysis_cache {}행 삭제", deleted);
        }
    }

    /**
     * 오래된 업로드 이미지 삭제 (기본: 매일 04:40).
     *
     * <p>이미지 분석 흐름은 저장 → OCR 텍스트 추출 → 이후 텍스트로만 분석이라,
     * 보관 일수가 지난 원본 파일은 참조되지 않는다. 지우지 않으면 디스크가 무한히 찬다.
     * 파일 나이는 수정 시각 기준(업로드 후 수정되지 않으므로 생성 시각과 동일).
     */
    @Scheduled(cron = "${storage.cleanup.upload-cron:0 40 4 * * *}")
    public void purgeOldUploads() {
        Path dir = Paths.get(uploadDir).toAbsolutePath();
        if (!Files.isDirectory(dir)) {
            return;
        }
        Instant threshold = Instant.now().minus(uploadRetentionDays, ChronoUnit.DAYS);

        int deleted = 0;
        long freedBytes = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                try {
                    if (!Files.isRegularFile(file)
                            || Files.getLastModifiedTime(file).toInstant().isAfter(threshold)) {
                        continue;
                    }
                    long size = Files.size(file);
                    Files.delete(file);
                    deleted++;
                    freedBytes += size;
                } catch (IOException e) {
                    // 파일 하나가 실패해도(사용 중 등) 나머지 정리는 계속한다.
                    log.warn("업로드 파일 삭제 실패: {} ({})", file.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("업로드 디렉토리 스캔 실패: {} ({})", dir, e.getMessage());
            return;
        }
        if (deleted > 0) {
            log.info("업로드 정리: {}일 지난 이미지 {}개 삭제 ({} KB 확보)",
                    uploadRetentionDays, deleted, freedBytes / 1024);
        }
    }
}
