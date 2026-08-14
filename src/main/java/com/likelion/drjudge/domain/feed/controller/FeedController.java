package com.likelion.drjudge.domain.feed.controller;

import com.likelion.drjudge.domain.auth.exception.AuthErrorCode;
import com.likelion.drjudge.domain.feed.dto.response.FeedPostPageResponse;
import com.likelion.drjudge.domain.feed.service.FeedService;
import com.likelion.drjudge.global.exception.BusinessException;
import com.likelion.drjudge.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feed/posts")
@RequiredArgsConstructor
@Validated
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<FeedPostPageResponse>> getMyFeedPosts(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Long userId = currentUserId();
        FeedPostPageResponse response = feedService.getMyFeedPosts(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // TODO: B1의 인증(JWT) 구현이 머지되면 실제 Principal 타입으로 교체.
    // Authentication을 메서드 파라미터로 받으면 Spring Security가 익명 요청의 principal을
    // null로 masking해버려서, SecurityContextHolder에서 직접 꺼내는 방식으로 우회한다.
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}