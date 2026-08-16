package com.likelion.drjudge.domain.judgment.dto.response;

public record ConflictOfInterestResponse(
        boolean detected,
        String type,
        String badgeLabel,
        String description
) {
}
