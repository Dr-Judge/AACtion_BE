package com.likelion.drjudge.domain.category.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * V1__init_schema.sql 의 categories 테이블 매핑.
 * 관심 카테고리 5종(NUTRITION, DIET, DISEASE_CARE, COSMETICS, PROCEDURE_SKINCARE)은
 * baseline 시드 데이터로 이미 들어가 있음 — 코드에서 새로 만들지 않는다.
 */
@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}