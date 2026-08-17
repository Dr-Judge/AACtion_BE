package com.likelion.drjudge.domain.briefing.controller;

import com.likelion.drjudge.domain.briefing.dto.response.BriefingResponse;
import com.likelion.drjudge.domain.briefing.service.BriefingService;
import com.likelion.drjudge.domain.jwt.service.CustomUserPrincipal;
import com.likelion.drjudge.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/briefings")
@RequiredArgsConstructor
public class BriefingController {

    private final BriefingService briefingService;

    @Operation(summary = "오늘의 브리핑 조회", description = "관심 카테고리를 우선 반영해 오늘의 브리핑 카드(최대 2개)를 조회한다.")
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<BriefingResponse>> getTodayBriefing(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        BriefingResponse response = briefingService.getTodayBriefing(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "특정 날짜 브리핑 조회", description = "지정한 날짜(YYYY-MM-DD)의 브리핑 카드를 관심 카테고리 우선으로 조회한다.")
    @GetMapping("/{date}")
    public ResponseEntity<ApiResponse<BriefingResponse>> getBriefingByDate(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        BriefingResponse response = briefingService.getBriefingByDate(principal.getId(), date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "브리핑 열람 기록", description = "브리핑 카드를 열람했음을 기록한다(오픈율 지표용). 이미 기록된 경우 중복 없이 성공 처리한다.")
    @PostMapping("/{briefingId}/open")
    public ResponseEntity<ApiResponse<Void>> recordBriefingOpen(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long briefingId
    ) {
        briefingService.recordBriefingOpen(principal.getId(), briefingId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}