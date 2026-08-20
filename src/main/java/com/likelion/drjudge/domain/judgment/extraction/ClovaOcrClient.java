package com.likelion.drjudge.domain.judgment.extraction;

import com.likelion.drjudge.domain.judgment.exception.JudgmentErrorCode;
import com.likelion.drjudge.global.exception.BusinessException;
import java.time.Instant;
import java.util.Base64;
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
    // data URI 접두사가 없는 순수 base64가 왔을 때, 디코딩한 실제 바이트의 매직 넘버로
    // 포맷을 추정한다 — 프론트가 "data:image/...;base64," 헤더를 잘라내고 순수 base64만
    // 보내는 경우가 실제로 있어서(클라이언트마다 관례가 다름), 접두사 유무와 무관하게
    // 실제 바이트를 근거로 판단하는 쪽이 더 견고하다.
    // PNG는 8바이트 서명 전체를 비교한다 - 앞 4바이트만 보면 89 50 4E 47 00 00 00 00처럼
    // PNG가 아닌 입력도 png로 잘못 분류될 수 있다.
    private static final byte[] PNG_MAGIC =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] TIFF_LE_MAGIC = {0x49, 0x49, 0x2A, 0x00};
    private static final byte[] TIFF_BE_MAGIC = {0x4D, 0x4D, 0x00, 0x2A};
    // RequestBodySizeFilter는 Content-Length 헤더 기반이라 chunked 요청은 못 막는다
    // (그 필터 자체의 문서화된 한계). 여기서는 그것과 무관하게, base64 디코딩으로 큰
    // byte[]를 할당하기 전에 자체적으로 한 번 더 길이를 제한한다.
    private static final int MAX_BASE64_LENGTH = 15_000_000; // 디코딩 시 약 11MB

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
     * "data:image/&lt;mime-subtype&gt;;base64,&lt;data&gt;" 형태면 그 MIME 타입을 그대로 쓴다.
     * 접두사가 없는 순수 base64면, 디코딩한 실제 바이트의 매직 넘버로 포맷을 추정한다.
     */
    ParsedImage parseDataUri(String imageBase64) {
        Matcher matcher = DATA_URI_PATTERN.matcher(imageBase64);
        if (matcher.matches()) {
            String mimeSubtype = matcher.group(1).toLowerCase();
            String format = MIME_SUBTYPE_TO_CLOVA_FORMAT.get(mimeSubtype);
            if (format == null) {
                log.warn("event=clova_ocr_unsupported_image_format mimeSubtype={}", mimeSubtype);
                throw new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED);
            }
            return new ParsedImage(format, matcher.group(2));
        }

        String format = sniffFormatFromBytes(imageBase64);
        if (format == null) {
            log.warn("event=clova_ocr_unsupported_image_format reason=unrecognized_bytes");
            throw new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED);
        }
        return new ParsedImage(format, imageBase64);
    }

    private String sniffFormatFromBytes(String base64) {
        if (base64.length() > MAX_BASE64_LENGTH) {
            log.warn("event=clova_ocr_image_too_large length={}", base64.length());
            return null;
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (startsWith(bytes, PNG_MAGIC)) {
            return "png";
        }
        if (startsWith(bytes, JPEG_MAGIC)) {
            return "jpg";
        }
        if (startsWith(bytes, TIFF_LE_MAGIC) || startsWith(bytes, TIFF_BE_MAGIC)) {
            return "tiff";
        }
        return null;
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    record ParsedImage(String format, String data) {
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
