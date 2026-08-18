package com.likelion.drjudge.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            KakaoProfile profile
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoProfile(
            String nickname
    ) {
    }

    public String resolveNickname() {
        if (kakaoAccount != null && kakaoAccount.profile() != null && kakaoAccount.profile().nickname() != null) {
            return kakaoAccount.profile().nickname();
        }
        return "카카오사용자";
    }
}