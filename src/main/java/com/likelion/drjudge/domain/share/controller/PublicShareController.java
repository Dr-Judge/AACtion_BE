package com.likelion.drjudge.domain.share.controller;

import com.likelion.drjudge.domain.share.dto.response.SharedJudgmentResponse;
import com.likelion.drjudge.domain.share.service.ShareService;
import com.likelion.drjudge.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class PublicShareController {

    private final ShareService shareService;

    @Operation(summary = "공유 링크 공개 조회", description = "비회원도 접근 가능한 공개 엔드포인트. 판정 결과와 근거를 조회하되 작성자 개인정보는 포함하지 않는다.")
    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<SharedJudgmentResponse>> getSharedJudgment(
            @PathVariable String token
    ) {
        SharedJudgmentResponse response = shareService.getSharedJudgment(token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}