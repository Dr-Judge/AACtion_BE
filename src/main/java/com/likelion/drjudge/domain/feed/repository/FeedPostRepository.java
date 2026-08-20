package com.likelion.drjudge.domain.feed.repository;

import com.likelion.drjudge.domain.feed.entity.FeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedPostRepository extends JpaRepository<FeedPost, Long> {

    Page<FeedPost> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    Page<FeedPost> findByIsPublicTrueOrderByCreatedAtDescIdDesc(Pageable pageable);

    Page<FeedPost> findByIsPublicTrueOrderByLikeCountDescIdDesc(Pageable pageable);
}
