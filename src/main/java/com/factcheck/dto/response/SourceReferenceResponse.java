package com.factcheck.dto.response;

import com.factcheck.Enum.SourceType;
import com.factcheck.domain.SourceReference;
import lombok.Getter;

@Getter
public class SourceReferenceResponse {

    private String sourceName;
    private String sourceUrl;
    private Float credibilityScore;
    private SourceType sourceType;

    public SourceReferenceResponse(SourceReference s) {
        this.sourceName = s.getSourceName();
        this.sourceUrl = s.getSourceUrl();
        this.credibilityScore = s.getCredibilityScore();
        this.sourceType = s.getSourceType();
    }
}
