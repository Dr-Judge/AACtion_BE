package com.likelion.drjudge.domain.feed.repository;

import com.likelion.drjudge.domain.feed.entity.FeedPost;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedPostRepository extends JpaRepository<FeedPost, Long> {

    List<FeedPost> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}