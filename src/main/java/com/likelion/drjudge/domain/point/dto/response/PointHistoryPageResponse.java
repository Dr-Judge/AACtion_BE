package com.likelion.drjudge.domain.point.dto.response;

import java.util.List;

public record PointHistoryPageResponse(
        List<PointHistoryResponse> items,
        boolean hasNext
) {
}