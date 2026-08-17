package com.likelion.drjudge.domain.briefing.dto.response;

import java.time.LocalDate;
import java.util.List;

public record BriefingResponse(
        LocalDate date,
        List<BriefingItemResponse> items
) {
}