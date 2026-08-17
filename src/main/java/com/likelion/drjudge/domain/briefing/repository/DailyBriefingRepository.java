package com.likelion.drjudge.domain.briefing.repository;

import com.likelion.drjudge.domain.briefing.entity.DailyBriefing;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * archive_items와 마찬가지로 읽기 전용 — 브리핑 후보 데이터는 관리자가 SQL로 직접 채운다.
 */
public interface DailyBriefingRepository extends Repository<DailyBriefing, Long> {

    List<DailyBriefing> findByBriefingDate(LocalDate briefingDate);
}