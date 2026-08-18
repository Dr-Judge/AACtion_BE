package com.likelion.drjudge.domain.briefing.repository;

import com.likelion.drjudge.domain.briefing.entity.BriefingView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BriefingViewRepository extends JpaRepository<BriefingView, Long> {

    boolean existsByUserIdAndDailyBriefingId(Long userId, Long dailyBriefingId);
}