package com.likelion.drjudge.domain.point.repository;

import com.likelion.drjudge.domain.point.entity.PointLedger;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface PointLedgerRepository extends JpaRepository<PointLedger, Long> {

    List<PointLedger> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}