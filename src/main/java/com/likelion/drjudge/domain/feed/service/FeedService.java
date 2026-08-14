package com.likelion.drjudge.domain.feed.service;

import com.likelion.drjudge.domain.feed.dto.response.FeedPostPageResponse;
import com.likelion.drjudge.domain.feed.dto.response.FeedPostResponse;
import com.likelion.drjudge.domain.feed.entity.FeedPost;
import com.likelion.drjudge.domain.feed.repository.FeedPostRepository;
import java.util.List;
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
}