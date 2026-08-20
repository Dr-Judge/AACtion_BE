package com.likelion.drjudge.domain.judgment.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.likelion.drjudge.global.exception.BusinessException;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class ClovaOcrClientTest {

    private final ClovaOcrClient client = new ClovaOcrClient("http://dummy", "dummy-secret");

    private String base64Of(int... unsignedBytes) {
        byte[] bytes = new byte[unsignedBytes.length];
        for (int i = 0; i < unsignedBytes.length; i++) {
            bytes[i] = (byte) unsignedBytes[i];
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void data_URI_접두사가_있으면_MIME_타입을_그대로_사용한다() {
        String raw = base64Of(0x89, 0x50, 0x4E, 0x47, 1, 2, 3);
        String input = "data:image/png;base64," + raw;

        ClovaOcrClient.ParsedImage parsed = client.parseDataUri(input);

        assertEquals("png", parsed.format());
        assertEquals(raw, parsed.data());
    }

    @Test
    void data_URI_접두사에_미지원_MIME이면_EXTRACTION_FAILED() {
        String input = "data:image/bmp;base64," + base64Of(1, 2, 3);

        assertThrows(BusinessException.class, () -> client.parseDataUri(input));
    }

    @Test
    void 접두사_없는_순수_base64여도_PNG_매직넘버면_png로_인식한다() {
        String raw = base64Of(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);

        ClovaOcrClient.ParsedImage parsed = client.parseDataUri(raw);

        assertEquals("png", parsed.format());
        assertEquals(raw, parsed.data());
    }

    @Test
    void 접두사_없는_순수_base64여도_JPEG_매직넘버면_jpg로_인식한다() {
        String raw = base64Of(0xFF, 0xD8, 0xFF, 0xE0, 1, 2, 3);

        ClovaOcrClient.ParsedImage parsed = client.parseDataUri(raw);

        assertEquals("jpg", parsed.format());
    }

    @Test
    void 접두사_없는_순수_base64여도_TIFF_매직넘버면_tiff로_인식한다() {
        String littleEndian = base64Of(0x49, 0x49, 0x2A, 0x00, 1, 2, 3);
        String bigEndian = base64Of(0x4D, 0x4D, 0x00, 0x2A, 1, 2, 3);

        assertEquals("tiff", client.parseDataUri(littleEndian).format());
        assertEquals("tiff", client.parseDataUri(bigEndian).format());
    }

    @Test
    void 접두사도_없고_알려진_매직넘버도_아니면_EXTRACTION_FAILED() {
        String raw = base64Of(0x00, 0x01, 0x02, 0x03);

        assertThrows(BusinessException.class, () -> client.parseDataUri(raw));
    }

    @Test
    void base64로도_디코딩되지_않는_문자열이면_EXTRACTION_FAILED() {
        assertThrows(BusinessException.class, () -> client.parseDataUri("!!! not base64 !!!"));
    }
}
