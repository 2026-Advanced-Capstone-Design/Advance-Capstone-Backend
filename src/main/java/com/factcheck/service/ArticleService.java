package com.factcheck.service;

import com.factcheck.Enum.InputType;
import com.factcheck.domain.AnalysisCache;
import com.factcheck.domain.Article;
import com.factcheck.dto.response.AnalyzeResponse;
import com.factcheck.dto.response.AnalysisResultResponse;
import com.factcheck.dto.response.AnalysisStatusResponse;
import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import com.factcheck.repository.AnalysisCacheRepository;
import com.factcheck.repository.AnalysisResultRepository;
import com.factcheck.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final AnalysisCacheRepository analysisCacheRepository;

    private static final String IMAGE_UPLOAD_DIR = "uploads/images/";

    /**
     * 분석 상태 조회
     * GET /api/v1/articles/{id}/status
     */
    public AnalysisStatusResponse getStatus(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        return new AnalysisStatusResponse(article);
    }

    /**
     * 분석 결과 조회
     * GET /api/v1/articles/{id}/result
     * 명세 FR-09: analysis_results + sentence_analyses + source_references JOIN
     */
    public AnalysisResultResponse getResult(Long articleId) {
        articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        return analysisResultRepository.findByArticleId(articleId)
                .map(AnalysisResultResponse::new)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESULT_NOT_FOUND));
    }

    // ── 입력 타입별 처리 ──────────────────────────────────────────────

    @Transactional
    public AnalyzeResponse submitText(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Article article = Article.builder()
                .inputType(InputType.TEXT)
                .originalText(text)
                .build();
        articleRepository.save(article);
        return new AnalyzeResponse(article);
    }

    @Transactional
    public AnalyzeResponse submitUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String urlHash = DigestUtils.md5DigestAsHex(url.getBytes());

        // 만료되지 않은 캐시가 있으면 기존 article_id 반환
        return analysisCacheRepository.findByUrlHash(urlHash)
                .filter(cache -> !cache.isExpired())
                .map(cache -> {
                    cache.incrementHitCount();
                    return new AnalyzeResponse(cache.getArticle());
                })
                .orElseGet(() -> {
                    Article article = Article.builder()
                            .inputType(InputType.URL)
                            .sourceUrl(url)
                            .build();
                    articleRepository.save(article);

                    analysisCacheRepository.save(AnalysisCache.builder()
                            .urlHash(urlHash)
                            .article(article)
                            .build());

                    return new AnalyzeResponse(article);
                });
    }

    @Transactional
    public AnalyzeResponse submitImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String imagePath = saveImage(image);
        Article article = Article.builder()
                .inputType(InputType.IMAGE)
                .imagePath(imagePath)
                .build();
        articleRepository.save(article);
        return new AnalyzeResponse(article);
    }

    private String saveImage(MultipartFile image) {
        try {
            Path uploadPath = Paths.get(IMAGE_UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            image.transferTo(filePath.toFile());
            return filePath.toString();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }
}
