package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.domain.Article;
import com.factcheck.repository.ArticleRepository;
import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OcrAsyncService {

    private final OcrService ocrService;
    private final PreprocessService preprocessService;
    private final AiWorkerClient aiWorkerClient;
    private final ArticleRepository articleRepository;
    private final Executor ocrExecutor;

    public OcrAsyncService(OcrService ocrService, PreprocessService preprocessService,
                           AiWorkerClient aiWorkerClient, ArticleRepository articleRepository,
                           @Qualifier("ocrExecutor") Executor ocrExecutor) {
        this.ocrService = ocrService;
        this.preprocessService = preprocessService;
        this.aiWorkerClient = aiWorkerClient;
        this.articleRepository = articleRepository;
        this.ocrExecutor = ocrExecutor;
    }

    @Async("ocrExecutor")
    @Transactional
    public void processOcrAsync(Long articleId, List<File> imageFiles) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        try {
            List<CompletableFuture<String>> futures = imageFiles.stream()
                    .map(imageFile -> CompletableFuture.supplyAsync(() -> {
                        try {
                            String text = ocrService.extractText(imageFile);
                            if (text.isBlank()) {
                                log.warn("OCR 결과 없음: articleId={}, file={}", articleId, imageFile.getName());
                                return "";
                            }
                            return String.join("\n", preprocessService.splitOnly(text));
                        } catch (Exception e) {
                            log.warn("OCR 처리 실패: file={}, error={}", imageFile.getName(), e.getMessage());
                            return "";
                        }
                    }, ocrExecutor))
                    .toList();

            String finalText = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(t -> !t.isBlank())
                    .collect(Collectors.joining("\n"))
                    .trim();

            log.info("병렬 OCR 완료: articleId={}, {}개 이미지, {}자", articleId, imageFiles.size(), finalText.length());
            article.updateOriginalText(finalText);
            aiWorkerClient.submitAnalysis(article);

        } catch (Exception e) {
            log.error("비동기 OCR 처리 실패: articleId={}, error={}", articleId, e.getMessage());
            article.updateStatus(ArticleStatus.FAILED);
        }
    }
}
