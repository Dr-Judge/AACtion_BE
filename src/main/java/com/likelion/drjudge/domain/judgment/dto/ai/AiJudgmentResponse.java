package com.likelion.drjudge.domain.judgment.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Python -> Spring 응답 바디. docs/AI_SERVICE_CONTRACT.md 계약과 1:1로 맞춘다 (snake_case).
 */
public record AiJudgmentResponse(
        String title,
        @JsonProperty("trust_level") String trustLevel,
        @JsonProperty("evidence_summary") String evidenceSummary,
        @JsonProperty("conflict_of_interest") ConflictOfInterest conflictOfInterest,
        @JsonProperty("safety_notice") String safetyNotice,
        List<Source> sources,
        @JsonProperty("guide_card") GuideCard guideCard
) {

    public record ConflictOfInterest(
            boolean detected,
            String type,
            String description
    ) {
    }

    public record Source(
            String title,
            String url,
            String publisher,
            String type
    ) {
    }

    public record GuideCard(
            String title,
            @JsonProperty("source_type") String sourceType,
            @JsonProperty("source_ref") String sourceRef,
            List<String> tips
    ) {
    }
}
