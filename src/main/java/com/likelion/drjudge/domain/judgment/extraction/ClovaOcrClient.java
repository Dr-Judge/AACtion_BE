package com.likelion.drjudge.domain.judgment.extraction;

import com.likelion.drjudge.domain.judgment.exception.JudgmentErrorCode;
import com.likelion.drjudge.global.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 네이버클라우드플랫폼 Clova OCR(일반 OCR) 연동. IMAGE 입력에서 텍스트를 뽑아낸다.
 */
@Slf4j
@Component
public class ClovaOcrClient {

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final Pattern DATA_URI_PATTERN = Pattern.compile("^data:image/([a-zA-Z]+);base64,(.+)$", Pattern.DOTALL);
    // Clova 일반 OCR API가 지원하는 형식만 매핑한다 (bmp는 미지원이라 뺌).
    private static final Map<String, String> MIME_SUBTYPE_TO_CLOVA_FORMAT = Map.of(
            "jpeg", "jpg",
            "jpg", "jpg",
            "png", "png",
            "tiff", "tiff"
    );

    private final RestClient restClient;
    private final String apiUrl;
    private final String secretKey;

    public ClovaOcrClient(
            @Value("${clova.ocr.api-url:}") String apiUrl,
            @Value("${clova.ocr.secret-key:}") String secretKey
    ) {
        this.apiUrl = apiUrl;
        this.secretKey = secretKey;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public String extractText(String imageBase64) {
        if (apiUrl.isBlank() || secretKey.isBlank()) {
            log.warn("event=clova_ocr_not_configured");
            throw new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED);
        }

        ParsedImage parsedImage = parseDataUri(imageBase64);

        OcrRequest request = new OcrRequest(
                "V2",
                UUID.randomUUID().toString(),
                Instant.now().toEpochMilli(),
                List.of(new OcrImage(parsedImage.format(), "image", parsedImage.data()))
        );

        try {
            OcrResponse response = restClient.post()
                    .uri(apiUrl)
                    .header("X-OCR-SECRET", secretKey)
                    .body(request)
                    .retrieve()
                    .body(OcrResponse.class);

            String text = extractJoinedText(response);
            if (text.isBlank()) {
                throw new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED);
            }
            return text;
        } catch (RestClientException e) {
            log.warn("event=clova_ocr_failed reason={}", e.getMessage());
            throw new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED);
        }
    }

    private String extractJoinedText(OcrResponse response) {
        if (response == null || response.images() == null || response.images().isEmpty()) {
            return "";
        }
        OcrImageResult image = response.images().get(0);
        if (!"SUCCESS".equals(image.inferResult()) || image.fields() == null) {
            return "";
        }
        return image.fields().stream()
                .map(OcrField::inferText)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining(" "));
    }

    /**
     * "data:image/&lt;mime-subtype&gt;;base64,&lt;data&gt;" 형태만 허용한다. MIME 타입을
     * 알 수 없는 원시 base64는 어떤 포맷으로 보내야 할지 몰라 형식 자체를 거부한다.
     */
    private ParsedImage parseDataUri(String imageBase64) {
        Matcher matcher = DATA_URI_PATTERN.matcher(imageBase64);
        if (!matcher.matches()) {
            log.warn("event=clova_ocr_unsupported_image_format reason=no_data_uri_prefix");
            throw new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED);
        }

        String mimeSubtype = matcher.group(1).toLowerCase();
        String format = MIME_SUBTYPE_TO_CLOVA_FORMAT.get(mimeSubtype);
        if (format == null) {
            log.warn("event=clova_ocr_unsupported_image_format mimeSubtype={}", mimeSubtype);
            throw new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED);
        }
        return new ParsedImage(format, matcher.group(2));
    }

    private record ParsedImage(String format, String data) {
    }

    private record OcrRequest(String version, String requestId, long timestamp, List<OcrImage> images) {
    }

    private record OcrImage(String format, String name, String data) {
    }

    private record OcrResponse(String version, String requestId, long timestamp, List<OcrImageResult> images) {
    }

    private record OcrImageResult(String uid, String name, String inferResult, String message,
                                   List<OcrField> fields) {
    }

    private record OcrField(String valueType, String inferText, double inferConfidence) {
    }
}
