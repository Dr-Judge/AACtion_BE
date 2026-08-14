package com.likelion.drjudge.domain.point.service;

import com.likelion.drjudge.domain.point.dto.response.PointHistoryPageResponse;
import com.likelion.drjudge.domain.point.dto.response.PointHistoryResponse;
import com.likelion.drjudge.domain.point.entity.PointLedger;
import com.likelion.drjudge.domain.point.repository.PointLedgerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointLedgerRepository pointLedgerRepository;

    /** GET /users/me/points/history */
    public PointHistoryPageResponse getPointHistory(Long userId, int page, int size) {
        // size보다 1개 더 조회 — 그 1개가 있으면 "다음 페이지 있음"이라는 뜻
        PageRequest pageRequest = PageRequest.of(
                page - 1, size + 1,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        List<PointLedger> rows = pointLedgerRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);

        boolean hasNext = rows.size() > size;
        List<PointLedger> pageRows = hasNext ? rows.subList(0, size) : rows;

        List<PointHistoryResponse> items = pageRows.stream()
                .map(PointHistoryResponse::from)
                .toList();

        return new PointHistoryPageResponse(items, hasNext);
    }
}