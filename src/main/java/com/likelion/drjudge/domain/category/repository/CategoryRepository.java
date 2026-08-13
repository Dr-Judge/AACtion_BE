package com.likelion.drjudge.domain.category.repository;

import com.likelion.drjudge.domain.category.entity.Category;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByCodeIn(Collection<String> codes);
}