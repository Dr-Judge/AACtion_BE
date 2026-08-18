package com.likelion.drjudge.domain.judgment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * completeJudgment()가 AiJudgmentResponse.GuideCard를 그대로 직렬화해서 저장하는데,
 * 그쪽은 snake_case(@JsonProperty)로 저장되므로 다시 읽어올 때도 동일하게 맞춰야 한다.
 */
public record GuideCardResponse(
        String title,
        @JsonProperty("source_type") String sourceType,
        @JsonProperty("source_ref") String sourceRef,
        List<String> tips
) {
}
