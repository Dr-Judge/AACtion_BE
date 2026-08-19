package com.likelion.drjudge.domain.judgment.service;

import java.util.regex.Pattern;

/**
 * 판정 엔진(AI 서비스)에 텍스트를 넘기기 전에 흔한 개인정보 패턴을 마스킹한다.
 * TEXT/IMAGE(OCR)/LINK(유튜브 설명) 입력 전부 이 경로를 거친다 — .coderabbit.yaml에
 * 명시된 정책("OCR/링크 추출 텍스트에 PII 마스킹이 판정 엔진 전달 전에 적용되는지 확인한다").
 */
final class PiiMasker {

    private static final Pattern PHONE = Pattern.compile("01[016789]-?\\d{3,4}-?\\d{4}");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern RESIDENT_REGISTRATION_NUMBER = Pattern.compile("\\d{6}-?[1-4]\\d{6}");

    private PiiMasker() {
    }

    static String mask(String text) {
        if (text == null) {
            return null;
        }
        String masked = PHONE.matcher(text).replaceAll("[전화번호 비공개]");
        masked = RESIDENT_REGISTRATION_NUMBER.matcher(masked).replaceAll("[주민등록번호 비공개]");
        masked = EMAIL.matcher(masked).replaceAll("[이메일 비공개]");
        return masked;
    }
}
