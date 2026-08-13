package com.likelion.drjudge.global.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrustLevel {

    CLINICAL_EVIDENCE("임상적 근거 있음"),
    EXPERT_OPINION("전문가 의견 있음"),
    PENDING("판단보류"),
    NO_EVIDENCE("근거 부족"),
    COUNTER_EVIDENCE("반박 근거 있음");

    private final String label;
}
