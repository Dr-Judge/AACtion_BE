package com.likelion.drjudge.domain.feed.repository;

import com.likelion.drjudge.domain.feed.entity.FeedLike;
import com.likelion.drjudge.domain.feed.entity.FeedLikeId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedLikeRepository extends JpaRepository<FeedLike, FeedLikeId> {

    // 목록 화면에서 게시물 N개마다 좋아요 여부를 매번 따로 조회하지 않도록,
    // 이 유저가 좋아요 누른 게시물 id만 한 번에 가져온다.
    List<FeedLike> findByIdUserIdAndIdFeedPostIdIn(Long userId, Collection<Long> feedPostIds);
}
