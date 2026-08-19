package com.likelion.drjudge.domain.judgment.extraction;

import com.likelion.drjudge.domain.judgment.exception.JudgmentErrorCode;
import com.likelion.drjudge.global.exception.BusinessException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 유튜브 링크에서 제목+설명을 뽑아온다 (YouTube Data API v3). 자막까지는 공식 API로
 * 비로그인 사용자 대상으로 못 가져와서(OAuth 필요) 다루지 않는다 — LINK 입력은
 * 유튜브만 지원하고, 그 외(인스타 릴스 등)는 공식 공개 API가 없어 EXTRACTION_FAILED로 처리한다.
 */
@Slf4j
@Component
public class YoutubeClient {

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    // youtube.com/watch?v=ID, youtu.be/ID, youtube.com/shorts/ID, youtube.com/embed/ID 형태를 모두 잡는다.
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|shorts/|embed/)|youtu\\.be/)([A-Za-z0-9_-]{11})"
    );

    private final RestClient restClient;
    private final String apiKey;

    public YoutubeClient(@Value("${youtube.api-key:}") String apiKey) {
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public String extractText(String url) {
        if (apiKey.isBlank()) {
            log.warn("event=youtube_api_not_configured");
            throw new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED);
        }

        String videoId = extractVideoId(url);
        if (videoId == null) {
            log.warn("event=youtube_unsupported_link url={}", url);
            throw new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED);
        }

        try {
            VideoListResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("www.googleapis.com").path("/youtube/v3/videos")
                            .queryParam("part", "snippet")
                            .queryParam("id", videoId)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(VideoListResponse.class);

            if (response == null || response.items() == null || response.items().isEmpty()) {
                throw new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED);
            }

            Snippet snippet = response.items().get(0).snippet();
            String combined = (snippet.title() + "\n" + snippet.description()).trim();
            if (combined.isBlank()) {
                throw new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED);
            }
            return combined;
        } catch (RestClientException e) {
            log.warn("event=youtube_extraction_failed reason={}", e.getMessage());
            throw new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED);
        }
    }

    private String extractVideoId(String url) {
        Matcher matcher = VIDEO_ID_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private record VideoListResponse(List<VideoItem> items) {
    }

    private record VideoItem(String id, Snippet snippet) {
    }

    private record Snippet(String title, String description) {
    }
}
