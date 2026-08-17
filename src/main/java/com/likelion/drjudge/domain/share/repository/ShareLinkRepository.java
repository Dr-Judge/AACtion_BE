package com.likelion.drjudge.domain.share.repository;

import com.likelion.drjudge.domain.share.entity.ShareLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    boolean existsByToken(String token);

    Optional<ShareLink> findByTokenAndIsActiveTrue(String token);

    Optional<ShareLink> findFirstByJudgmentIdAndUserIdAndIsActiveTrueOrderByCreatedAtDesc(Long judgmentId, Long userId);
}