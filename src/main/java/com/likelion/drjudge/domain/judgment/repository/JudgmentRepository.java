package com.likelion.drjudge.domain.judgment.repository;

import com.likelion.drjudge.domain.category.entity.Category;
import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JudgmentRepository extends JpaRepository<Judgment, Long> {

    @EntityGraph(attributePaths = "category")
    Slice<Judgment> findAllByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Slice<Judgment> findAllByUserAndCategoryOrderByCreatedAtDesc(User user, Category category, Pageable pageable);
}
