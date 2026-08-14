package com.likelion.drjudge.domain.judgment.dto.response;

import com.likelion.drjudge.domain.judgment.entity.InputType;
import com.likelion.drjudge.domain.judgment.entity.JudgmentStatus;
import java.time.LocalDateTime;

public record JudgmentSummaryResponse(
        Long judgmentId,
        JudgmentStatus status,
        String trustLevel,
        String trustLevelLabel,
        InputType inputType,
        Long categoryId,
        LocalDateTime createdAt
) {
}
