package com.likelion.drjudge.domain.feed.controller;

import com.likelion.drjudge.domain.feed.dto.request.FeedPostCreateRequest;
import com.likelion.drjudge.domain.feed.dto.response.FeedPostResponse;
import com.likelion.drjudge.domain.jwt.service.CustomUserPrincipal;
import com.likelion.drjudge.domain.feed.dto.response.FeedPostPageResponse;
import com.likelion.drjudge.domain.feed.service.FeedService;
import com.likelion.drjudge.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed/posts")
@RequiredArgsConstructor
@Validated
public class FeedController {

    private final FeedService feedService;

    @Operation(summary = "공유 피드 전체 목록 조회", description = "공개된 공유 카드 전체 목록을 조회한다. sort=recent(기본, 최신순)|popular(좋아요순).")
    @GetMapping
    public ResponseEntity<ApiResponse<FeedPostPageResponse>> getFeedPosts(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "recent") @Pattern(regexp = "recent|popular") String sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        FeedPostPageResponse response = feedService.getFeedPosts(principal.getId(), sort, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내가 올린 공유 카드 목록 조회", description = "로그인한 사용자가 공유 피드에 게시한 판정 카드 목록을 최신순으로 조회한다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<FeedPostPageResponse>> getMyFeedPosts(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        FeedPostPageResponse response = feedService.getMyFeedPosts(principal.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공유 피드에 게시", description = "완료된 판정 결과를 공유 피드에 게시한다. 본인 소유이면서 상태가 COMPLETED인 판정만 게시 가능하다.")
    @PostMapping
    public ResponseEntity<ApiResponse<FeedPostResponse>> createFeedPost(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody FeedPostCreateRequest request
    ) {
        FeedPostResponse response = feedService.createFeedPost(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "공유 피드 게시물 삭제", description = "본인이 게시한 공유 카드를 삭제한다.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deleteFeedPost(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long postId
    ) {
        feedService.deleteFeedPost(principal.getId(), postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "공유 피드 좋아요", description = "게시물에 좋아요를 누른다. 이미 눌렀으면 409(FEED_002)를 반환한다.")
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Void>> likeFeedPost(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long postId
    ) {
        feedService.likeFeedPost(principal.getId(), postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "공유 피드 좋아요 취소", description = "눌렀던 좋아요를 취소한다. 누른 적 없으면 400(FEED_007)을 반환한다.")
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeFeedPost(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long postId
    ) {
        feedService.unlikeFeedPost(principal.getId(), postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}