package com.factcheck.dto.response;

import com.factcheck.domain.AnalysisResult;
import com.factcheck.domain.AnalysisSection;
import com.factcheck.domain.FactCheckResult;
import com.factcheck.domain.SentenceAnalysis;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AnalysisResultResponse {

    private Long articleId;
    private Long resultId;
    private Integer totalScore;
    private String originalText;
    private Indicators indicators;
    private BiasInfo bias;
    private SummaryInfo summary;
    private CotReasons cotReasons;
    private List<SectionInfo> sections;
    private List<SentenceInfo> sentences;
    private List<FactCheckInfo> factChecks;
    private LocalDateTime analyzedAt;

    public AnalysisResultResponse(AnalysisResult result) {
        this.articleId    = result.getArticle().getId();
        this.resultId     = result.getId();
        this.totalScore   = result.getTotalScore();
        this.originalText = result.getCompressedText();
        this.indicators   = new Indicators(result);
        this.bias         = new BiasInfo(result);
        this.summary      = new SummaryInfo(result);
        this.cotReasons   = new CotReasons(result);
        this.sections     = result.getSections().stream().map(SectionInfo::new).toList();
        this.sentences    = result.getSentences().stream().map(SentenceInfo::new).toList();
        this.factChecks   = result.getFactCheckResults().stream().map(FactCheckInfo::new).toList();
        this.analyzedAt   = result.getAnalyzedAt();
    }

    @Getter
    public static class Indicators {
        private Float factRatio;
        private Float emotionNeutrality;
        private Float omissionNeutrality;
        private Float biasScore;

        public Indicators(AnalysisResult r) {
            this.factRatio          = r.getFactRatio();
            this.emotionNeutrality  = r.getEmotionNeutrality();
            this.omissionNeutrality = null;
            this.biasScore          = r.getBiasScore();
        }
    }

    @Getter
    public static class BiasInfo {
        private String biasDirection;
        private String spectrumLabel;
        private String biasReason;

        public BiasInfo(AnalysisResult r) {
            this.biasDirection = r.getBiasDirection();
            this.spectrumLabel = r.getBiasLabel();
            this.biasReason    = r.getBiasReason();
        }
    }

    @Getter
    public static class SummaryInfo {
        private String title;
        private String keywords;

        public SummaryInfo(AnalysisResult r) {
            this.title    = r.getTitle();
            this.keywords = r.getKeywords();
        }
    }

    @Getter
    public static class CotReasons {
        private String emotionNeutrality;
        private String factRatio;

        public CotReasons(AnalysisResult r) {
            this.emotionNeutrality = r.getCotEmotionReason();
            this.factRatio         = r.getCotFactRatioReason();
        }
    }

    @Getter
    public static class SectionInfo {
        private String topic;
        private String biasLabel;
        private Float confidence;
        private String reason;

        public SectionInfo(AnalysisSection s) {
            this.topic      = s.getTopic();
            this.biasLabel  = s.getBiasLabel();
            this.confidence = s.getConfidence();
            this.reason     = s.getReason();
        }
    }

    @Getter
    public static class SentenceInfo {
        private String sentenceText;
        private String highlightType;
        private String highlightReason;
        private Float highlightScore;

        public SentenceInfo(SentenceAnalysis s) {
            this.sentenceText    = s.getSentenceText();
            this.highlightType   = s.getHighlightType();
            this.highlightReason = s.getHighlightReason();
            this.highlightScore  = s.getHighlightScore();
        }
    }

    @Getter
    public static class FactCheckInfo {
        private String fact;
        private Boolean found;
        private String rating;
        private Float score;
        private String title;
        private String publisher;
        private String url;

        public FactCheckInfo(FactCheckResult fc) {
            this.fact      = fc.getFact();
            this.found     = fc.getFound();
            this.rating    = fc.getRating();
            this.score     = fc.getScore();
            this.title     = fc.getTitle();
            this.publisher = fc.getPublisher();
            this.url       = fc.getUrl();
        }
    }
}
