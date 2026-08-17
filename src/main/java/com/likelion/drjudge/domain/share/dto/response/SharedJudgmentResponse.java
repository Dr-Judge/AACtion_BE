package com.likelion.drjudge.domain.share.dto.response;

import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.global.constant.TrustLevel;

public record SharedJudgmentResponse(
        Long judgmentId,
        TrustLevel trustLevel,
        String evidenceSummary,
        boolean conflictDetected,
        String conflictDescription,
        String safetyNotice,
        String guideCardJson
) {
    public static SharedJudgmentResponse from(Judgment judgment) {
        return new SharedJudgmentResponse(
                judgment.getId(),
                judgment.getTrustLevel(),
                judgment.getEvidenceSummary(),
                judgment.isConflictDetected(),
                judgment.getConflictDescription(),
                judgment.getSafetyNotice(),
                judgment.getGuideCardJson()
        );
    }
}