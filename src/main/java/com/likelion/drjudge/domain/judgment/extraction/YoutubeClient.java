package com.likelion.drjudge.domain.judgment.extraction;

import com.likelion.drjudge.domain.judgment.exception.JudgmentErrorCode;
import com.likelion.drjudge.global.exception.BusinessException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
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

    private static final Set<String> ALLOWED_HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be");
    private static final Pattern VIDEO_ID_FORMAT = Pattern.compile("[A-Za-z0-9_-]{11}");

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

    /**
     * 문자열 부분일치가 아니라 실제 host를 파싱해서 검사한다 — 그냥 "youtube.com" 부분
     * 문자열 포함 여부만 보면 https://evil.com/youtube.com/watch?v=... 같은 비유튜브
     * URL도 통과해버린다.
     */
    private String extractVideoId(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return null;
        }

        String host = uri.getHost();
        if (host == null || !ALLOWED_HOSTS.contains(host.toLowerCase())) {
            return null;
        }

        String path = uri.getPath() == null ? "" : uri.getPath();
        String candidate;
        if ("youtu.be".equalsIgnoreCase(host)) {
            candidate = stripLeadingSlash(path);
        } else if (path.startsWith("/shorts/")) {
            candidate = path.substring("/shorts/".length());
        } else if (path.startsWith("/embed/")) {
            candidate = path.substring("/embed/".length());
        } else if ("/watch".equals(path)) {
            candidate = queryParam(uri.getQuery(), "v");
        } else {
            return null;
        }

        if (candidate == null) {
            return null;
        }
        int nextSlash = candidate.indexOf('/');
        if (nextSlash != -1) {
            candidate = candidate.substring(0, nextSlash);
        }
        return VIDEO_ID_FORMAT.matcher(candidate).matches() ? candidate : null;
    }

    private String stripLeadingSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String queryParam(String query, String key) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq != -1 && pair.substring(0, eq).equals(key)) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

    private record VideoListResponse(List<VideoItem> items) {
    }

    private record VideoItem(String id, Snippet snippet) {
    }

    private record Snippet(String title, String description) {
    }
}
