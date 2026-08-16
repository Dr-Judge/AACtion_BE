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

import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.judgment.entity.JudgmentStatus;
import com.likelion.drjudge.domain.judgment.exception.JudgmentErrorCode;
import com.likelion.drjudge.domain.judgment.repository.JudgmentRepository;
import com.likelion.drjudge.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    /** GET /feed/posts/me */
    public FeedPostPageResponse getMyFeedPosts(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page - 1, size + 1,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        List<FeedPost> rows = feedPostRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);

        boolean hasNext = rows.size() > size;
        List<FeedPost> pageRows = hasNext ? rows.subList(0, size) : rows;

        List<FeedPostResponse> items = pageRows.stream()
                .map(FeedPostResponse::from)
                .toList();

        return new FeedPostPageResponse(items, hasNext);
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

        FeedPost feedPost = FeedPost.create(judgment, judgment.getUser());
        feedPostRepository.save(feedPost);

        entityManager.flush();
        entityManager.refresh(feedPost);

        return FeedPostResponse.from(feedPost);
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