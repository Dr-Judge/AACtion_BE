package com.likelion.drjudge.domain.judgment.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Spring -> Python 요청 바디. docs/AI_SERVICE_CONTRACT.md 계약과 1:1로 맞춘다 (snake_case).
 */
public record AiJudgmentRequest(
        String text,
        @JsonProperty("category_id") Long categoryId
) {
}
