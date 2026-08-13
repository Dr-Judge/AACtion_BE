package com.likelion.drjudge.domain.archive.repository;

import com.likelion.drjudge.domain.archive.entity.ArchiveItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * 읽기 전용 사용을 전제로 한다 — 등록/수정/삭제는 관리자가 SQL로 직접 처리한다(R-TUSYUO).
 * archive_items는 여러 도메인이 참조하는 공용 테이블이라(DB_MIGRATION_RULES.md 7번),
 * 브리핑(briefings)·판정(judgment) 등 다른 도메인이 이 Repository를 직접 주입해 사용해도 된다.
 * JpaRepository 대신 Repository 마커 인터페이스를 써서 save/delete가 아예 노출되지 않게 막는다 —
 * 다른 도메인 트랜잭션에서 실수로 공용 데이터를 변경하는 걸 컴파일 타임에 차단한다.
 */
public interface ArchiveItemRepository extends Repository<ArchiveItem, Long> {

    Optional<ArchiveItem> findById(Long id);

    List<ArchiveItem> findAllById(Iterable<Long> ids);
}
