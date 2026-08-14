package com.likelion.drjudge.domain.judgment.repository;

import com.likelion.drjudge.domain.category.entity.Category;
import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JudgmentRepository extends JpaRepository<Judgment, Long> {

    // createdAt만으로는 동시각 생성 시 순서가 안정적이지 않아 id를 보조 정렬로 둔다 (load-more 페이지 경계 누락/중복 방지).
    @EntityGraph(attributePaths = "category")
    Slice<Judgment> findAllByUserOrderByCreatedAtDescIdDesc(User user, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Slice<Judgment> findAllByUserAndCategoryOrderByCreatedAtDescIdDesc(
            User user, Category category, Pageable pageable);
}
