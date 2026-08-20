package com.likelion.drjudge.domain.briefing.service;

import com.likelion.drjudge.domain.archive.entity.ArchiveItem;
import com.likelion.drjudge.domain.archive.repository.ArchiveItemRepository;
import com.likelion.drjudge.domain.briefing.entity.DailyBriefing;
import com.likelion.drjudge.domain.briefing.repository.DailyBriefingRepository;
import com.likelion.drjudge.domain.category.entity.Category;
import com.likelion.drjudge.domain.category.repository.CategoryRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * daily_briefings는 채워주는 관리 도구가 없어서(archive_items의 sync_archive_csv.py 같은 것)
 * 계속 비어있었다 — /briefings/today가 매일 빈 배열만 내려주던 원인. 매일 자정(KST)에
 * 카테고리별로 archive_items에서 몇 개씩 뽑아 오늘 날짜로 자동 채운다.
 * 이미 그 날짜 데이터가 있으면 건드리지 않는다(멱등) — 재배포/재시작 때마다 중복 생성 방지.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyBriefingGenerator {

    private static final int ITEMS_PER_CATEGORY = 2;

    private final DailyBriefingRepository dailyBriefingRepository;
    private final ArchiveItemRepository archiveItemRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionTemplate transactionTemplate;

    /** 서버가 배포/재시작될 때 오늘자 브리핑이 비어있으면 즉시 채운다 (자정까지 기다릴 필요 없이). */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        generateForDate(LocalDate.now());
    }

    /** 매일 자정(KST)에 그날 브리핑을 생성한다. */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void generateDaily() {
        generateForDate(LocalDate.now());
    }

    /**
     * 같은 빈 안에서 self-invocation(onStartup/generateDaily가 이 메서드를 직접 호출)하면
     * @Transactional 프록시를 안 거쳐서 무효가 된다 — TransactionTemplate으로 명시적으로
     * 감싼다. 카테고리 5개 중 일부만 저장하고 실패하면, 그 절반만 커밋된 상태로
     * existsByBriefingDate가 true가 돼버려 그 날짜는 영영 미완성으로 남는다.
     */
    public void generateForDate(LocalDate date) {
        transactionTemplate.executeWithoutResult(status -> generateForDateInTransaction(date));
    }

    private void generateForDateInTransaction(LocalDate date) {
        if (dailyBriefingRepository.existsByBriefingDate(date)) {
            return;
        }

        List<Category> categories = categoryRepository.findAll();
        int created = 0;
        for (Category category : categories) {
            List<ArchiveItem> candidates =
                    archiveItemRepository.findRandomByCategoryId(category.getId(), ITEMS_PER_CATEGORY);
            for (ArchiveItem item : candidates) {
                dailyBriefingRepository.save(DailyBriefing.create(date, item.getId(), category.getId()));
                created++;
            }
        }
        log.info("event=daily_briefing_generated date={} count={}", date, created);
    }
}
