package com.likelion.drjudge.domain.briefing.service;

import com.likelion.drjudge.domain.archive.entity.ArchiveItem;
import com.likelion.drjudge.domain.archive.repository.ArchiveItemRepository;
import com.likelion.drjudge.domain.briefing.dto.response.BriefingItemResponse;
import com.likelion.drjudge.domain.briefing.dto.response.BriefingResponse;
import com.likelion.drjudge.domain.briefing.entity.DailyBriefing;
import com.likelion.drjudge.domain.briefing.repository.DailyBriefingRepository;
import com.likelion.drjudge.domain.category.entity.Category;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.exception.UserErrorCode;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import com.likelion.drjudge.global.exception.BusinessException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BriefingService {

    private static final int MAX_ITEMS = 2;

    private final UserRepository userRepository;
    private final DailyBriefingRepository dailyBriefingRepository;
    private final ArchiveItemRepository archiveItemRepository;

    /** GET /briefings/today */
    public BriefingResponse getTodayBriefing(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        Set<Long> interestCategoryIds = user.getInterestCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        List<DailyBriefing> todayBriefings = dailyBriefingRepository.findByBriefingDate(today);

        // 관심 카테고리 우선(8), 나머지로 부족분 채움(2) — 최대 MAX_ITEMS개
        List<DailyBriefing> matched = todayBriefings.stream()
                .filter(b -> interestCategoryIds.contains(b.getCategoryId()))
                .toList();
        List<DailyBriefing> others = todayBriefings.stream()
                .filter(b -> !interestCategoryIds.contains(b.getCategoryId()))
                .toList();

        List<DailyBriefing> selected = new ArrayList<>();
        selected.addAll(matched.stream().limit(MAX_ITEMS).toList());
        if (selected.size() < MAX_ITEMS) {
            int remain = MAX_ITEMS - selected.size();
            selected.addAll(others.stream().limit(remain).toList());
        }

        List<Long> archiveItemIds = selected.stream()
                .map(DailyBriefing::getArchiveItemId)
                .toList();

        List<ArchiveItem> archiveItems = new ArrayList<>(archiveItemRepository.findAllById(archiveItemIds));

        List<BriefingItemResponse> items = archiveItems.stream()
                .map(BriefingItemResponse::from)
                .toList();

        return new BriefingResponse(today, items);
    }
}