package com.likelion.drjudge.domain.archive.repository;

import com.likelion.drjudge.domain.archive.entity.ArchiveItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

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

    // 카테고리별 전체 목록을 애플리케이션으로 가져와 셔플하면 아카이브가 커질수록
    // 조회 시간·메모리가 같이 늘어난다 — DB에서 바로 N개만 무작위로 뽑는다.
    @Query(value = "SELECT * FROM archive_items WHERE category_id = :categoryId ORDER BY RAND() LIMIT :limit",
            nativeQuery = true)
    List<ArchiveItem> findRandomByCategoryId(@Param("categoryId") Long categoryId, @Param("limit") int limit);
}
