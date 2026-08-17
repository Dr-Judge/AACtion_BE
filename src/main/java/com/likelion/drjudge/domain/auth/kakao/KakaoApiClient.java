package com.likelion.drjudge.domain.auth.kakao;

import com.likelion.drjudge.domain.auth.dto.response.KakaoUserInfoResponse;
import com.likelion.drjudge.domain.auth.exception.AuthErrorCode;
import com.likelion.drjudge.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KakaoApiClient {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();

    public KakaoUserInfoResponse getUserInfo(String kakaoAccessToken) {
        try {
            return restClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
        } catch (RestClientException e) {
            log.warn("event=kakao_userinfo_failed reason={}", e.getMessage());
            throw new BusinessException(AuthErrorCode.KAKAO_AUTH_FAILED);
        }
    }
}