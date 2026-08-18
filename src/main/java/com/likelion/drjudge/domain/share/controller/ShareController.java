package com.likelion.drjudge.domain.share.controller;

import com.likelion.drjudge.domain.jwt.service.CustomUserPrincipal;
import com.likelion.drjudge.domain.share.dto.response.ShareLinkResponse;
import com.likelion.drjudge.domain.share.service.ShareService;
import com.likelion.drjudge.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/judgments/{judgmentId}/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @Operation(summary = "공유 링크 생성", description = "완료된 판정 결과의 공유 링크를 생성한다. 본인 소유이면서 상태가 COMPLETED인 판정만 공유 가능하다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ShareLinkResponse>> createShareLink(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long judgmentId
    ) {
        ShareLinkResponse response = shareService.createShareLink(principal.getId(), judgmentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "공유 링크 회수", description = "발급된 공유 링크를 비활성화한다. 이후 해당 링크로 조회 시 404를 반환한다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> revokeShareLink(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long judgmentId
    ) {
        shareService.revokeShareLink(principal.getId(), judgmentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}