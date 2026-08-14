package com.likelion.drjudge.domain.judgment.dto.response;

import java.util.List;

public record GuideCardResponse(
        String title,
        String sourceType,
        String sourceRef,
        List<String> tips
) {
}
