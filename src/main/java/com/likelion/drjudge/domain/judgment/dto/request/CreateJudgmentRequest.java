package com.likelion.drjudge.domain.judgment.dto.request;

import com.likelion.drjudge.domain.judgment.entity.InputType;
import jakarta.validation.constraints.NotNull;

public record CreateJudgmentRequest(
        @NotNull InputType inputType,
        String text,
        String imageBase64,
        String url,
        Long categoryId
) {
}
