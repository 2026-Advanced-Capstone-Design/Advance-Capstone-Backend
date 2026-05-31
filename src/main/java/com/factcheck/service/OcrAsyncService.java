package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.domain.Article;
import com.factcheck.repository.ArticleRepository;
import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrAsyncService {

    private final OcrService ocrService;
    private final PreprocessService preprocessService;
    private final AiWorkerClient aiWorkerClient;
    private final ArticleRepository articleRepository;

    @Async("ocrExecutor")
    @Transactional
    public void processOcrAsync(Long articleId, List<File> imageFiles) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        try {
            StringBuilder combined = new StringBuilder();
            for (File imageFile : imageFiles) {
                String ocrText = ocrService.extractText(imageFile);
                if (!ocrText.isBlank()) {
                    List<String> sentences = preprocessService.splitOnly(ocrText);
                    combined.append(String.join("\n", sentences)).append("\n");
                } else {
                    log.warn("비동기 OCR 결과 없음: articleId={}, file={}", articleId, imageFile.getName());
                }
            }

            String finalText = combined.toString().trim();
            log.info("비동기 OCR 완료: articleId={}, {}개 이미지, {}자", articleId, imageFiles.size(), finalText.length());

            article.updateOriginalText(finalText);
            aiWorkerClient.submitAnalysis(article);

        } catch (Exception e) {
            log.error("비동기 OCR 처리 실패: articleId={}, error={}", articleId, e.getMessage());
            article.updateStatus(ArticleStatus.FAILED);
        }
    }
}
