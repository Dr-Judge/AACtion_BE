package com.likelion.drjudge.domain.point.entity;

import com.likelion.drjudge.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "points_ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PointReason reason;

    @Column(nullable = false)
    private int amount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}