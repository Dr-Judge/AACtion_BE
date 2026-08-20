package com.likelion.drjudge.domain.feed.service;

import com.likelion.drjudge.domain.feed.dto.request.FeedPostCreateRequest;
import com.likelion.drjudge.domain.feed.dto.response.FeedPostPageResponse;
import com.likelion.drjudge.domain.feed.dto.response.FeedPostResponse;
import com.likelion.drjudge.domain.feed.entity.FeedLike;
import com.likelion.drjudge.domain.feed.entity.FeedLikeId;
import com.likelion.drjudge.domain.feed.entity.FeedPost;
import com.likelion.drjudge.domain.feed.exception.FeedErrorCode;
import com.likelion.drjudge.domain.feed.repository.FeedLikeRepository;
import com.likelion.drjudge.domain.feed.repository.FeedPostRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.judgment.entity.JudgmentStatus;
import com.likelion.drjudge.domain.judgment.exception.JudgmentErrorCode;
import com.likelion.drjudge.domain.judgment.repository.JudgmentRepository;
import com.likelion.drjudge.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedPostRepository feedPostRepository;
    private final JudgmentRepository judgmentRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final EntityManager entityManager;

    /** GET /feed/posts — sort=recent(기본)|popular */
    public FeedPostPageResponse getFeedPosts(Long userId, String sort, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);

        Page<FeedPost> result = "popular".equals(sort)
                ? feedPostRepository.findByIsPublicTrueOrderByLikeCountDescIdDesc(pageRequest)
                : feedPostRepository.findByIsPublicTrueOrderByCreatedAtDescIdDesc(pageRequest);

        return toPageResponse(result, userId);
    }

    /** GET /feed/posts/me */
    public FeedPostPageResponse getMyFeedPosts(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);

        Page<FeedPost> result = feedPostRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageRequest);

        return toPageResponse(result, userId);
    }

    private FeedPostPageResponse toPageResponse(Page<FeedPost> result, Long viewerId) {
        List<Long> postIds = result.getContent().stream().map(FeedPost::getId).toList();
        Set<Long> likedPostIds = feedLikeRepository.findByIdUserIdAndIdFeedPostIdIn(viewerId, postIds).stream()
                .map(like -> like.getId().getFeedPostId())
                .collect(Collectors.toSet());

        List<FeedPostResponse> items = result.getContent().stream()
                .map(post -> FeedPostResponse.from(post, likedPostIds.contains(post.getId())))
                .toList();

        return new FeedPostPageResponse(items, result.getNumber() + 1, Math.max(result.getTotalPages(), 1));
    }

    @Transactional
    public FeedPostResponse createFeedPost(Long userId, FeedPostCreateRequest request) {
        Judgment judgment = judgmentRepository.findById(request.judgmentId())
                .orElseThrow(() -> new BusinessException(JudgmentErrorCode.JUDGMENT_NOT_FOUND));

        if (!judgment.getUser().getId().equals(userId)) {
            throw new BusinessException(FeedErrorCode.NOT_JUDGMENT_OWNER);
        }
        if (judgment.getStatus() != JudgmentStatus.COMPLETED) {
            throw new BusinessException(FeedErrorCode.JUDGMENT_NOT_COMPLETED);
        }
        if (feedPostRepository.existsByJudgmentId(judgment.getId())) {
            throw new BusinessException(FeedErrorCode.ALREADY_POSTED);
        }

        FeedPost feedPost = FeedPost.create(judgment, judgment.getUser());
        feedPostRepository.save(feedPost);

        judgment.getUser().increasePoint(20);

        entityManager.flush();
        entityManager.refresh(feedPost);

        return FeedPostResponse.from(feedPost, false);
    }

    @Transactional
    public void deleteFeedPost(Long userId, Long postId) {
        FeedPost feedPost = feedPostRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_NOT_FOUND));

        if (!feedPost.getUser().getId().equals(userId)) {
            throw new BusinessException(FeedErrorCode.NOT_POST_OWNER);
        }

        feedPostRepository.delete(feedPost);
    }

    @Transactional
    public void likeFeedPost(Long userId, Long postId) {
        FeedPost feedPost = feedPostRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_NOT_FOUND));

        FeedLikeId likeId = new FeedLikeId(postId, userId);
        if (feedLikeRepository.existsById(likeId)) {
            throw new BusinessException(FeedErrorCode.ALREADY_LIKED);
        }

        feedLikeRepository.save(FeedLike.create(postId, userId));
        feedPost.increaseLikeCount();
    }

    @Transactional
    public void unlikeFeedPost(Long userId, Long postId) {
        FeedLikeId likeId = new FeedLikeId(postId, userId);
        FeedLike feedLike = feedLikeRepository.findById(likeId)
                .orElseThrow(() -> new BusinessException(FeedErrorCode.LIKE_NOT_FOUND));

        feedLikeRepository.delete(feedLike);

        feedPostRepository.findById(postId)
                .ifPresent(FeedPost::decreaseLikeCount);
    }
}
