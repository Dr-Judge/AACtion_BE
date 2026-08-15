package com.likelion.drjudge.domain.feed.repository;

import com.likelion.drjudge.domain.feed.entity.FeedLike;
import com.likelion.drjudge.domain.feed.entity.FeedLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedLikeRepository extends JpaRepository<FeedLike, FeedLikeId> {
}