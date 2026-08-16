package com.likelion.drjudge.domain.judgment.dto.response;

import com.likelion.drjudge.domain.judgment.entity.InputType;
import com.likelion.drjudge.domain.judgment.entity.JudgmentStatus;
import java.time.LocalDateTime;
import java.util.List;

public record JudgmentDetailResponse(
        Long judgmentId,
        JudgmentStatus status,
        String trustLevel,
        String trustLevelLabel,
        String evidenceSummary,
        ConflictOfInterestResponse conflictOfInterest,
        String safetyNotice,
        List<SourceResponse> sources,
        GuideCardResponse guideCard,
        String extractedText,
        String errorCode,
        String errorMessage,
        InputType inputType,
        Long categoryId,
        LocalDateTime createdAt
) {
}
