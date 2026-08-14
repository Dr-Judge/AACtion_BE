package com.likelion.drjudge.domain.judgment.dto.response;

import java.util.List;

public record JudgmentListResponse(
        List<JudgmentSummaryResponse> items,
        boolean hasNext
) {
}
