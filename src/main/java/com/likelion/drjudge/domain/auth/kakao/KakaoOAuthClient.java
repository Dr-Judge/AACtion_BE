package com.likelion.drjudge.domain.auth.kakao;

import com.likelion.drjudge.domain.auth.dto.response.KakaoTokenResponse;
import com.likelion.drjudge.domain.auth.exception.AuthErrorCode;
import com.likelion.drjudge.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KakaoOAuthClient {

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";

    private final RestClient restClient = RestClient.create();

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    public String exchangeCodeForAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            body.add("client_secret", clientSecret);
        }

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(KAKAO_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new BusinessException(AuthErrorCode.KAKAO_AUTH_FAILED);
            }
            return response.accessToken();
        } catch (RestClientException e) {
            log.warn("event=kakao_token_exchange_failed reason={}", e.getMessage());
            throw new BusinessException(AuthErrorCode.KAKAO_AUTH_FAILED);
        }
    }
}