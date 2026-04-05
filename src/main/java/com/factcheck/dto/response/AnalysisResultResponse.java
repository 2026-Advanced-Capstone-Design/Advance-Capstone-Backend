package com.factcheck.dto.response;

import com.factcheck.domain.AnalysisResult;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GET /api/v1/articles/{id}/result 응답 DTO
 * 명세 FR-09: 종합 결과 JSON (신뢰도, 4대 지표, 편향, 요약, 문장별 하이라이트, 출처)
 */
@Getter
public class AnalysisResultResponse {

    private Long articleId;
    private Long resultId;

    /** 신뢰도 종합 점수 (0~100) */
    private Integer totalScore;

    /** 4대 지표 */
    private Indicators indicators;

    /** 편향 분석 */
    private BiasInfo bias;

    /** 요약 정보 */
    private SummaryInfo summary;

    /** 문장별 하이라이트 목록 (FR-09: 문장별 하이라이트) */
    private List<SentenceAnalysisResponse> sentences;

    /** 출처 목록 (FR-08: 매칭된 출처 목록 + 일치 여부) */
    private List<SourceReferenceResponse> sources;

    private LocalDateTime analyzedAt;

    public AnalysisResultResponse(AnalysisResult result) {
        this.articleId   = result.getArticle().getId();
        this.resultId    = result.getId();
        this.totalScore  = result.getTotalScore();
        this.indicators  = new Indicators(result);
        this.bias        = new BiasInfo(result);
        this.summary     = new SummaryInfo(result);
        this.analyzedAt  = result.getAnalyzedAt();
        this.sentences   = result.getSentenceAnalyses().stream()
                .map(SentenceAnalysisResponse::new)
                .collect(Collectors.toList());
        this.sources     = result.getSourceReferences().stream()
                .map(SourceReferenceResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * 4대 지표
     * - 감정 중립성 (emotion_neutrality)
     * - 사실 비율   (fact_ratio)
     * - 출처 균형   (source_balance)
     * - 편향 점수   (bias_score)
     */
    @Getter
    public static class Indicators {
        private Float emotionNeutrality;
        private Float factRatio;
        private Float sourceBalance;
        private Float biasScore;

        public Indicators(AnalysisResult result) {
            this.emotionNeutrality = result.getEmotionNeutrality();
            this.factRatio         = result.getFactRatio();
            this.sourceBalance     = result.getSourceBalance();
            this.biasScore         = result.getBiasScore();
        }
    }

    /** 편향 방향 + 스펙트럼 분류 */
    @Getter
    public static class BiasInfo {
        private String biasDirection;
        private String spectrumLabel;
        private String biaSentence;   // 편향 문장 목록 (JSON string)

        public BiasInfo(AnalysisResult result) {
            this.biasDirection = result.getBiasDirection();
            this.spectrumLabel = result.getSpectrumLabel();
            this.biaSentence   = result.getBiaSentence();
        }
    }

    /** AI가 추출한 제목 + 요약 */
    @Getter
    public static class SummaryInfo {
        private String title;
        private String content;

        public SummaryInfo(AnalysisResult result) {
            this.title   = result.getTitle();
            this.content = result.getSummary();
        }
    }
}
