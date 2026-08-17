package com.likelion.drjudge.domain.briefing.controller;

import com.likelion.drjudge.domain.briefing.dto.response.BriefingResponse;
import com.likelion.drjudge.domain.briefing.service.BriefingService;
import com.likelion.drjudge.domain.jwt.service.CustomUserPrincipal;
import com.likelion.drjudge.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}