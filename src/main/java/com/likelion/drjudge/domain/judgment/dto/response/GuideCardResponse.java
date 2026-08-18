package com.likelion.drjudge.domain.judgment.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/**
 * completeJudgment()가 AiJudgmentResponse.GuideCard를 그대로 직렬화해서 저장하는데,
 * 그쪽은 snake_case(@JsonProperty)로 저장되므로 DB에서 다시 읽어올 때 그 이름도 인식해야 한다.
 * @JsonAlias는 역직렬화(읽기)에만 적용되고 API 응답 직렬화(쓰기)는 그대로 camelCase를
 * 유지한다 — @JsonProperty를 쓰면 프론트로 나가는 응답까지 snake_case로 바뀌어버린다.
 */
public record GuideCardResponse(
        String title,
        @JsonAlias("source_type") String sourceType,
        @JsonAlias("source_ref") String sourceRef,
        List<String> tips
) {
}
