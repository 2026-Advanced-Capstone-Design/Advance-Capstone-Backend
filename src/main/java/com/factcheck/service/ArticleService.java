package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.domain.Article;
import com.factcheck.dto.request.TextRequest;
import com.factcheck.dto.request.UrlRequest;
import com.factcheck.dto.response.AnalyzeResponse;
import com.factcheck.dto.response.AnalysisResultResponse;
import com.factcheck.dto.response.AnalysisStatusResponse;
import com.factcheck.dto.response.LatestArticleIdResponse;
import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import com.factcheck.repository.AnalysisCacheRepository;
import com.factcheck.repository.AnalysisResultRepository;
import com.factcheck.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final AnalysisCacheRepository analysisCacheRepository;

    private final CrawlerService crawlerService;
    private final OcrService ocrService;
    private final OcrAsyncService ocrAsyncService;
    private final PreprocessService preprocessService;
    private final AiWorkerClient aiWorkerClient;

    private static final String IMAGE_UPLOAD_DIR = "uploads/images/";

    public AnalysisStatusResponse getStatus(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        return new AnalysisStatusResponse(article);
    }

    @Transactional(readOnly = true)
    public AnalysisResultResponse getResult(Long articleId) {
        articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        return analysisResultRepository.findByArticleId(articleId)
                .map(AnalysisResultResponse::new)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESULT_NOT_FOUND));
    }

    public LatestArticleIdResponse getLatestAnalyzedArticleId() {
        return analysisResultRepository.findTopByOrderByIdDesc()
                .map(result -> new LatestArticleIdResponse(result.getArticle().getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESULT_NOT_FOUND));
    }

    // 입력 타입별 처리

    @Transactional
    public AnalyzeResponse submitText(TextRequest request) {
        List<String> sentences = preprocessService.splitOnly(request.getText());
        String processed = String.join("\n", sentences);
        log.info("텍스트 전처리 완료: 원문 {}자 → {}개 문장", request.getText().length(), sentences.size());

        Article article = request.toEntity(processed);
        articleRepository.save(article);

        runAfterCommit(() -> aiWorkerClient.submitAnalysis(article));
        return new AnalyzeResponse(article);
    }

    @Transactional
    public AnalyzeResponse submitUrl(UrlRequest request) {
        String url = request.getUrl();
        String urlHash = DigestUtils.md5DigestAsHex(url.getBytes());

        return analysisCacheRepository.findByUrlHash(urlHash)
                // 캐시가 만료 되지 않은 경우만
                .filter(cache -> !cache.isExpired())
                // DONE된 기사만 캐시 히트로 인정.
                // 왜? 분석중이나, 실패 기사를 캐시로 넣으면 고장난 기사만 쓰게 되니깐
                .filter(cache -> cache.getArticle().getStatus() == ArticleStatus.DONE)

                .map(cache -> {
                    cache.incrementHitCount();
                    return new AnalyzeResponse(cache.getArticle());
                })
                .orElseGet(() -> {
                    Map<String, String> crawled = crawlerService.extractFromUrl(url);
                    String body = crawled.get("body");
                    String title = crawled.get("title");

                    if (body == null || body.isBlank()) {
                        log.warn("URL 크롤링 결과 본문 없음: {}", url);
                        throw new BusinessException(ErrorCode.CRAWL_FAILED);
                    }
                    List<String> sentences = preprocessService.splitOnly(body);
                    String processedText = String.join("\n", sentences);
                    log.info("URL 크롤링 완료: {} - {}자", url, processedText.length());

                    Article article = request.toEntity(title, processedText);
                    articleRepository.save(article);

                    runAfterCommit(() -> aiWorkerClient.submitAnalysis(article));
                    return new AnalyzeResponse(article);
                });
    }

    @Transactional
    public AnalyzeResponse submitImage(List<MultipartFile> images) {
        if (images == null || images.isEmpty() || images.stream().allMatch(MultipartFile::isEmpty)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        List<java.io.File> savedFiles = images.stream()
                .filter(img -> !img.isEmpty())
                .map(img -> saveImage(img).toFile())
                .toList();

        Article article = saveImageArticle(savedFiles.get(0).getPath());

        Long articleId = article.getId();
        runAfterCommit(() -> ocrAsyncService.processOcrAsync(articleId, savedFiles));

        return new AnalyzeResponse(article);
    }

    // 현재 트랜젝션에 콜백을 등록,
    private void runAfterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private Article saveImageArticle(String imagePath) {
        Article article = Article.createFromImage(imagePath, "");
        return articleRepository.save(article);
    }

    private Path saveImage(MultipartFile image) {
        try {
            Path uploadPath = Paths.get(IMAGE_UPLOAD_DIR).toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            log.info("이미지 저장 시도: uploadPath={}, fileName={}", uploadPath, fileName);

            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("이미지 저장 성공: {}", filePath);
            return filePath;
        } catch (IOException e) {
            log.error("이미지 저장 실패: dir={}, fileName={}, error={}",
                    IMAGE_UPLOAD_DIR, image.getOriginalFilename(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }
}
