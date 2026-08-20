package com.likelion.drjudge.domain.briefing.dto.response;

import com.likelion.drjudge.domain.archive.entity.ArchiveItem;
import com.likelion.drjudge.global.constant.TrustLevel;

public record BriefingItemResponse(
        Long briefingId,
        String categoryCode,
        TrustLevel trustLevel,
        String target,
        String effect,
        String evidenceSummary
) {
    public static BriefingItemResponse from(Long briefingId, ArchiveItem archiveItem, String categoryCode) {
        return new BriefingItemResponse(
                briefingId,
                categoryCode,
                archiveItem.getTrustLevel(),
                archiveItem.getTarget(),
                archiveItem.getEffect(),
                archiveItem.getEvidenceSummary()
        );
    }
}