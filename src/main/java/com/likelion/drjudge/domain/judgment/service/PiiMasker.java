package com.likelion.drjudge.domain.judgment.service;

import java.util.regex.Pattern;

/**
 * 판정 엔진(AI 서비스)에 텍스트를 넘기기 전에 흔한 개인정보 패턴을 마스킹한다.
 * TEXT/IMAGE(OCR)/LINK(유튜브 설명) 입력 전부 이 경로를 거친다 — .coderabbit.yaml에
 * 명시된 정책("OCR/링크 추출 텍스트에 PII 마스킹이 판정 엔진 전달 전에 적용되는지 확인한다").
 */
final class PiiMasker {

    // 주민등록번호(13자리 연속)를 먼저 마스킹해야 한다 — 전화번호 패턴을 먼저 돌리면
    // 주민번호 중간의 "010..." 비슷한 부분 문자열을 전화번호로 오인해 일부만 마스킹하고
    // 나머지 숫자는 그대로 새어나간다. \b로 숫자 경계를 둬서 더 긴 숫자열의 일부만
    // 잘못 매칭하는 것도 막는다.
    private static final Pattern RESIDENT_REGISTRATION_NUMBER = Pattern.compile("\\b\\d{6}[-\\s]?[1-4]\\d{6}\\b");
    // 휴대폰(010/011/016~019), 서울(02), 지역번호(031~069 등 0[3-6]x) 형태를 커버한다.
    private static final Pattern PHONE = Pattern.compile("\\b0(?:2|1[016789]|[3-6]\\d)[-\\s]?\\d{3,4}[-\\s]?\\d{4}\\b");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");

    private PiiMasker() {
    }

    static String mask(String text) {
        if (text == null) {
            return null;
        }
        String masked = RESIDENT_REGISTRATION_NUMBER.matcher(text).replaceAll("[주민등록번호 비공개]");
        masked = PHONE.matcher(masked).replaceAll("[전화번호 비공개]");
        masked = EMAIL.matcher(masked).replaceAll("[이메일 비공개]");
        return masked;
    }
}
