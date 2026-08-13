package com.likelion.drjudge.domain.point.dto.response;

import com.likelion.drjudge.domain.point.entity.PointLedger;
import com.likelion.drjudge.domain.point.entity.PointReason;
import java.time.LocalDateTime;

public record PointHistoryResponse(
        Long id,
        PointReason reason,
        int amount,
        LocalDateTime createdAt
) {
    public static PointHistoryResponse from(PointLedger pointLedger) {
        return new PointHistoryResponse(
                pointLedger.getId(),
                pointLedger.getReason(),
                pointLedger.getAmount(),
                pointLedger.getCreatedAt()
        );
    }
}