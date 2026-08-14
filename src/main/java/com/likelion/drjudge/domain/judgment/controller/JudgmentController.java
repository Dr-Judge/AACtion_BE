package com.likelion.drjudge.domain.judgment.controller;

import com.likelion.drjudge.domain.judgment.dto.request.CreateJudgmentRequest;
import com.likelion.drjudge.domain.judgment.dto.response.CreateJudgmentResponse;
import com.likelion.drjudge.domain.judgment.dto.response.JudgmentDetailResponse;
import com.likelion.drjudge.domain.judgment.dto.response.JudgmentListResponse;
import com.likelion.drjudge.domain.judgment.exception.JudgmentErrorCode;
import com.likelion.drjudge.domain.judgment.service.JudgmentService;
import com.likelion.drjudge.global.exception.BusinessException;
import com.likelion.drjudge.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/judgments")
@RequiredArgsConstructor
public class JudgmentController {

    private final JudgmentService judgmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateJudgmentResponse>> create(
            @Valid @RequestBody CreateJudgmentRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId(httpRequest);
        CreateJudgmentResponse response = judgmentService.create(userId, request);
        judgmentService.processAsync(response.judgmentId());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
    }

    @GetMapping("/{judgmentId}")
    public ResponseEntity<ApiResponse<JudgmentDetailResponse>> get(
            @PathVariable Long judgmentId,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(judgmentService.get(userId, judgmentId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<JudgmentListResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(judgmentService.list(userId, categoryId, page, size)));
    }

    /**
     * TODO(auth): 인증 PR 머지되면 실제 방식(예: @AuthenticationPrincipal)으로 교체.
     * 지금은 SecurityConfig/JWT 필터가 없어서 임시로 헤더에서 직접 읽는다 — 교체 지점을 여기 하나로 격리해둠.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-Debug-User-Id");
        if (userIdHeader == null) {
            throw new BusinessException(JudgmentErrorCode.ANONYMOUS_JUDGMENT_NOT_ALLOWED);
        }
        return Long.parseLong(userIdHeader);
    }
}
