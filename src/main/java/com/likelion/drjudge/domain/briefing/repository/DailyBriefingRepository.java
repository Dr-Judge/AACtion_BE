package com.likelion.drjudge.domain.briefing.repository;

import com.likelion.drjudge.domain.briefing.entity.DailyBriefing;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyBriefingRepository extends JpaRepository<DailyBriefing, Long> {

    List<DailyBriefing> findByBriefingDateOrderByIdAsc(LocalDate briefingDate);

    boolean existsByBriefingDate(LocalDate briefingDate);
}