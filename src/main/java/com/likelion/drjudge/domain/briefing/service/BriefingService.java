package com.likelion.drjudge.domain.briefing.service;

import com.likelion.drjudge.domain.archive.entity.ArchiveItem;
import com.likelion.drjudge.domain.archive.repository.ArchiveItemRepository;
import com.likelion.drjudge.domain.briefing.dto.response.BriefingItemResponse;
import com.likelion.drjudge.domain.briefing.dto.response.BriefingResponse;
import com.likelion.drjudge.domain.briefing.entity.BriefingView;
import com.likelion.drjudge.domain.briefing.entity.DailyBriefing;
import com.likelion.drjudge.domain.briefing.exception.BriefingErrorCode;
import com.likelion.drjudge.domain.briefing.repository.BriefingViewRepository;
import com.likelion.drjudge.domain.briefing.repository.DailyBriefingRepository;
import com.likelion.drjudge.domain.category.entity.Category;
import com.likelion.drjudge.domain.category.repository.CategoryRepository;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.exception.UserErrorCode;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import com.likelion.drjudge.global.exception.BusinessException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BriefingService {

    private static final int MAX_ITEMS = 2;

    private final UserRepository userRepository;
    private final DailyBriefingRepository dailyBriefingRepository;
    private final ArchiveItemRepository archiveItemRepository;
    private final CategoryRepository categoryRepository;
    private final BriefingViewRepository briefingViewRepository;

    /** GET /briefings/today */
    public BriefingResponse getTodayBriefing(Long userId) {
        return getBriefingByDate(userId, LocalDate.now());
    }

    /** GET /briefings/{date} */
    public BriefingResponse getBriefingByDate(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        Set<Long> interestCategoryIds = user.getInterestCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        List<DailyBriefing> dateBriefings = dailyBriefingRepository.findByBriefingDateOrderByIdAsc(date);

        List<DailyBriefing> matched = dateBriefings.stream()
                .filter(b -> interestCategoryIds.contains(b.getCategoryId()))
                .toList();
        List<DailyBriefing> others = dateBriefings.stream()
                .filter(b -> !interestCategoryIds.contains(b.getCategoryId()))
                .toList();

        List<DailyBriefing> selected = new ArrayList<>();
        selected.addAll(matched.stream().limit(MAX_ITEMS).toList());
        if (selected.size() < MAX_ITEMS) {
            int remain = MAX_ITEMS - selected.size();
            selected.addAll(others.stream().limit(remain).toList());
        }

        List<Long> archiveItemIds = selected.stream().map(DailyBriefing::getArchiveItemId).toList();
        Map<Long, ArchiveItem> archiveItemsById = archiveItemRepository.findAllById(archiveItemIds).stream()
                .collect(Collectors.toMap(ArchiveItem::getId, item -> item));

        List<Long> categoryIds = selected.stream().map(DailyBriefing::getCategoryId).distinct().toList();
        Map<Long, String> categoryCodesById = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getCode));

        List<BriefingItemResponse> items = new ArrayList<>();
        for (DailyBriefing briefing : selected) {
            ArchiveItem archiveItem = archiveItemsById.get(briefing.getArchiveItemId());
            if (archiveItem == null) {
                log.warn("event=briefing_archive_item_missing briefingId={} archiveItemId={}",
                        briefing.getId(), briefing.getArchiveItemId());
                continue;
            }
            String categoryCode = categoryCodesById.get(briefing.getCategoryId());
            items.add(BriefingItemResponse.from(archiveItem, categoryCode));
        }

        return new BriefingResponse(date, items);
    }

    /** POST /briefings/{briefingId}/open */
    @Transactional
    public void recordBriefingOpen(Long userId, Long briefingId) {
        if (!dailyBriefingRepository.existsById(briefingId)) {
            throw new BusinessException(BriefingErrorCode.DAILY_BRIEFING_NOT_FOUND);
        }
        if (briefingViewRepository.existsByUserIdAndDailyBriefingId(userId, briefingId)) {
            return; // 이미 기록된 열람 — 중복 집계 방지, 에러 없이 그냥 성공 처리
        }
        briefingViewRepository.save(BriefingView.create(userId, briefingId));
    }
}