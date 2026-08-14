package com.likelion.drjudge.domain.judgment.dto.response;

import com.likelion.drjudge.domain.judgment.entity.JudgmentStatus;

public record CreateJudgmentResponse(
        Long judgmentId,
        JudgmentStatus status
) {
}
