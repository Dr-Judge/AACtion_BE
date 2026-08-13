package com.likelion.drjudge.domain.archive.repository;

import com.likelion.drjudge.domain.archive.entity.ArchiveItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 읽기 전용 사용을 전제로 한다 — 등록/수정/삭제는 관리자가 SQL로 직접 처리한다(R-TUSYUO).
 * archive_items는 여러 도메인이 참조하는 공용 테이블이라(DB_MIGRATION_RULES.md 7번),
 * 브리핑(briefings)·판정(judgment) 등 다른 도메인이 이 Repository를 직접 주입해 사용해도 된다.
 */
public interface ArchiveItemRepository extends JpaRepository<ArchiveItem, Long> {
}
