package com.likelion.drjudge.domain.judgment.controller;

import com.likelion.drjudge.domain.judgment.dto.request.CreateJudgmentRequest;
import com.likelion.drjudge.domain.judgment.dto.response.CreateJudgmentResponse;
import com.likelion.drjudge.domain.judgment.dto.response.JudgmentDetailResponse;
import com.likelion.drjudge.domain.judgment.dto.response.JudgmentListResponse;
import com.likelion.drjudge.domain.judgment.service.JudgmentService;
import com.likelion.drjudge.domain.jwt.service.CustomUserPrincipal;
import com.likelion.drjudge.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "판정(Judgment)", description = "건강/영양 주장에 대한 AI 판정 요청·조회 API")
@RestController
@RequestMapping("/api/judgments")
@RequiredArgsConstructor
public class JudgmentController {

    private final JudgmentService judgmentService;

    @Operation(
            summary = "판정 요청 생성",
            description = "TEXT/IMAGE/LINK 중 하나로 판정을 요청한다. 즉시 202로 judgmentId를 반환하고, "
                    + "실제 판정은 비동기로 처리된다. IMAGE는 Clova OCR로, LINK는 유튜브만 지원(YouTube Data API로 "
                    + "제목+설명 추출, 인스타 등 비지원 링크나 API 키 미설정 시 422 EXTRACTION_FAILED).")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateJudgmentResponse>> create(
            @Valid @RequestBody CreateJudgmentRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        CreateJudgmentResponse response = judgmentService.create(principal.getId(), request);
        judgmentService.processAsync(response.judgmentId());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
    }

    @Operation(
            summary = "판정 결과 조회 (폴링)",
            description = "status에 따라 응답 필드가 달라진다 — PROCESSING/COMPLETED/FAILED.")
    @GetMapping("/{judgmentId}")
    public ResponseEntity<ApiResponse<JudgmentDetailResponse>> get(
            @PathVariable Long judgmentId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(judgmentService.get(principal.getId(), judgmentId)));
    }

    @Operation(
            summary = "판정 목록 조회 (더보기)",
            description = "totalCount 없이 hasNext만 반환하는 load-more 방식. 일일 요청 한도에 포함되지 않는다.")
    @GetMapping
    public ResponseEntity<ApiResponse<JudgmentListResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                ApiResponse.success(judgmentService.list(principal.getId(), categoryId, page, size)));
    }
}
