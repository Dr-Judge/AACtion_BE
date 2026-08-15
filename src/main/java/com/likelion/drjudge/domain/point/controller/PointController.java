package com.likelion.drjudge.domain.point.controller;

import com.likelion.drjudge.domain.jwt.service.CustomUserPrincipal;
import com.likelion.drjudge.domain.point.dto.response.PointHistoryPageResponse;
import com.likelion.drjudge.domain.point.service.PointService;
import com.likelion.drjudge.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/points")
@RequiredArgsConstructor
@Validated
public class PointController {

    private final PointService pointService;

    @Operation(summary = "포인트 적립 내역 조회", description = "로그인한 사용자의 포인트 적립/사용 내역을 최신순으로 조회한다. page/size 기반 더보기 페이지네이션(hasNext)을 사용한다.")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PointHistoryPageResponse>> getPointHistory(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PointHistoryPageResponse response = pointService.getPointHistory(principal.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}