package com.likelion.drjudge.domain.user.entity;

import com.likelion.drjudge.domain.category.entity.Category;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kakao_id", nullable = false, unique = true, length = 50)
    private String kakaoId;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", length = 20)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "point_balance", nullable = false)
    private int pointBalance;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_interests",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> interestCategories = new HashSet<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public void completeOnboarding(Set<Category> categories, AgeGroup ageGroup, Gender gender) {
        this.interestCategories.clear();
        this.interestCategories.addAll(categories);
        this.ageGroup = ageGroup;
        this.gender = gender;
    }

    public void updateInterestCategories(Set<Category> categories) {
        this.interestCategories.clear();
        this.interestCategories.addAll(categories);
    }

    public void updateAgeGroup(AgeGroup ageGroup) {
        this.ageGroup = ageGroup;
    }

    public void updateGender(Gender gender) {
        this.gender = gender;
    }

    public boolean isOnboardingCompleted() {
        return ageGroup != null && gender != null && !interestCategories.isEmpty();
    }
}