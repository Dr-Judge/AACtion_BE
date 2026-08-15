package com.likelion.drjudge.domain.auth.dto.response;

import java.time.LocalDateTime;

public record WithdrawResponse(
        LocalDateTime withdrawnAt
) {
}