# ErrorCode 정책 (Dr-Judge)

`@RestControllerAdvice` + `CustomException` + `ErrorCode enum` 조합으로 예외를 처리한다.
권한 예외, 존재하지 않는 리소스 예외 등은 별도 정책 없이 이 체계(GlobalExceptionHandler + ErrorCode) 안에서 관리한다.

## 응답 포맷

```json
{
  "status": 404,
  "errorCode": "JUDGMENT_001",
  "message": "존재하지 않는 판정입니다.",
  "path": "/api/judgments/1",
  "timestamp": "2026-08-08T10:30:00"
}
```

## GlobalExceptionHandler 로깅

```java
log.warn("event=exception_handled reason={}, code={}, message={}, traceId={}, details={}",
        exception.getErrorCode().name(),
        exception.getErrorCode().getCode(),
        exception.getMessage(),
        traceId,
        exception.getContext()
);
```

## 공통

| 코드 | 의미 | HTTP |
|---|---|---|
| COMMON_400 | 잘못된 요청 | 400 |
| COMMON_401 | 인증 실패 | 401 |
| COMMON_403 | 권한 없음 | 403 |
| COMMON_404 | 리소스 없음 | 404 |
| COMMON_500 | 서버 오류 | 500 |

## AUTH (임수영 담당)

| 코드 | 의미 | HTTP |
|---|---|---|
| AUTH_001 | 만료된 토큰 | 401 |
| AUTH_002 | 유효하지 않은 토큰 | 401 |
| AUTH_003 | 카카오 인증 실패 | 401 |

## USER

| 코드 | 의미 | HTTP |
|---|---|---|
| USER_001 | 존재하지 않는 사용자 | 404 |
| USER_002 | 이미 탈퇴한 사용자 | 409 |

## JUDGMENT (김현지 담당)

| 코드 | 의미 | HTTP |
|---|---|---|
| JUDGMENT_001 | 존재하지 않는 판정 | 404 |
| JUDGMENT_002 | 비회원 판정 시도 | 401 |
| JUDGMENT_003 | 일일 판정 요청 한도 초과 | 429 |
| JUDGMENT_004 | 입력 텍스트 추출 실패(OCR/링크) | 422 |

## FEED (조은서 담당)

| 코드 | 의미 | HTTP |
|---|---|---|
| FEED_001 | 존재하지 않는 게시물 | 404 |
| FEED_002 | 이미 좋아요 누른 게시물 | 409 |
| FEED_003 | 비공개 처리된 카드 접근 | 403 |

## SHARE (조은서 담당)

| 코드 | 의미 | HTTP |
|---|---|---|
| SHARE_001 | 존재하지 않거나 회수된 공유링크 | 404 |

## POINT (조은서 담당)

| 코드 | 의미 | HTTP |
|---|---|---|
| POINT_001 | 이미 오늘 출석체크 완료 | 409 |

## ARCHIVE (김현지 담당)

| 코드 | 의미 | HTTP |
|---|---|---|
| ARCHIVE_001 | 존재하지 않는 아카이브 항목 | 404 |

## 네이밍 원칙

- 에러 코드는 `{도메인}_{3자리 숫자}` 형식
- 에러 이름은 의미 중심으로 (`USER_NOT_FOUND` 같은 서술형은 `errorCode` enum 상수명으로, 문자열 코드값과 별도 관리 가능)
- HTTP 상태 코드는 표준 의미를 따른다
