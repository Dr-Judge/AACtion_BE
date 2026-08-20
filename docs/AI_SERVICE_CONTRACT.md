# AI 서비스 연동 계약 (Spring ↔ Python)

> 대상: 백엔드 3인 협업 (Dr-Judge Spring 서비스 ↔ 판정 AI 서비스)
> 목적: 판정(Judgment) 도메인에서 Spring이 Python AI 서비스를 호출하는 방식을 고정한다.
> 주의: 이 문서는 **백엔드 내부 서버간 통신 계약**이다. 프론트엔드가 호출하는 `/api/judgments` 계열 API 명세서와는 별개이며, 여기 나오는 엔드포인트/필드는 프론트에 노출되지 않는다.

---

## 1. 시스템 경계

| | Spring | Python |
|---|---|---|
| 클라이언트 진입점 / 인증 | ✅ | ❌ |
| 영속화(MySQL) | ✅ | ❌ (예외: 5번 참조) |
| 비동기 처리 / 재시도 오케스트레이션 | ✅ | ❌ |
| 입력 정규화 (OCR, YouTube 메타데이터/자막 추출) | ✅ | ❌ |
| RAG 검색 (아카이브 벡터 유사도 검색) | ❌ | ✅ |
| LLM 추론 (Gemini 호출, 프롬프트, 응답 구조화) | ❌ | ✅ |

**핵심 원칙**: Python이 받는 입력은 항상 **순수 텍스트**다. 원본이 TEXT/IMAGE/LINK 무엇이었든, Spring이 OCR·메타데이터 추출로 정규화를 끝낸 뒤에만 Python을 호출한다. Python은 `inputType`을 알 필요도, 이미지/URL을 직접 다룰 필요도 없다.

```text
TEXT  ──────────────┐
IMAGE → Clova OCR ───┼─→ 순수 텍스트 ──→ Python (RAG + Gemini)
LINK  → 메타데이터/자막 추출 ┘
```

---

## 2. 엔드포인트

```http
POST /internal/api/v1/judgments
```

### 요청 헤더

| 헤더 | 값 | 비고 |
|---|---|---|
| `X-Internal-Api-Key` | 공유 시크릿 | Spring↔Python 양쪽에 동일하게 설정, 각자 시크릿 파일로 관리 |
| `Content-Type` | `application/json` | |

### 요청 바디 (snake_case)

```json
{
  "text": "이 영양제 먹으면 관절통이 싹 낫는다더라",
  "category_id": 3
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `text` | string | ✅ | Spring이 정규화 완료한 순수 텍스트 |
| `category_id` | integer \| null | ❌ | RAG 검색 범위를 해당 카테고리로 좁히는 용도. null이면 전체 아카이브 대상 검색 |

### 응답 200 (snake_case)

```json
{
  "title": "이 영양제를 먹으면 관절통이 완치될까?",
  "trust_level": "COUNTER_EVIDENCE",
  "evidence_summary": "특정 성분이 관절통을 완치시킨다는 임상 근거는 확인되지 않았습니다.",
  "conflict_of_interest": {
    "detected": true,
    "type": "COMMERCIAL",
    "description": "..."
  },
  "safety_notice": null,
  "sources": [
    { "title": "...", "url": "...", "publisher": "...", "type": "CLINICAL_STUDY" }
  ],
  "guide_card": {
    "title": "...",
    "source_type": "...",
    "source_ref": "...",
    "tips": ["...", "..."]
  }
}
```

**의도적으로 제외한 필드**: `trust_level_label`, 배지 한글 라벨류. Python은 `trust_level` enum 값만 반환하고, 한글 라벨 매핑은 Spring이 소유한다 (Python이 UI 카피를 알 필요 없게 분리).

`trust_level` 값은 Spring의 `TrustLevel` enum과 1:1로 맞춘다: `CLINICAL_EVIDENCE` / `EXPERT_OPINION` / `PENDING` / `COUNTER_EVIDENCE` / `NO_EVIDENCE`.

### 응답 필드 null / 필수 규칙

**원칙: 필드를 생략하지 않는다.** 값이 없으면 `null`(단일 값) 또는 `[]`(배열)로 채워서 항상 모든 키를 내려준다 — Spring 쪽에서 필드 존재 여부까지 분기하지 않아도 되게.

| 필드 | 필수 | null 허용 | 규칙 |
|---|---|---|---|
| `title` | ✅ | ❌ | 사용자 주장을 의문문 형태로 요약한 한 줄 제목. 판정 이력·공유 카드에 노출됨 (예: "이 영양제를 먹으면 관절통이 완치될까?") |
| `trust_level` | ✅ | ❌ | 항상 5종 enum 중 하나 |
| `evidence_summary` | ✅ | ❌ | 빈 문자열 금지 |
| `conflict_of_interest.detected` | ✅ | ❌ | - |
| `conflict_of_interest.type` | ✅(키 존재) | `detected: false`면 `null` | |
| `conflict_of_interest.description` | ✅(키 존재) | `detected: false`면 `null` | |
| `safety_notice` | ✅(키 존재) | `trust_level`이 `PENDING`/`NO_EVIDENCE`가 아니면 `null` | |
| `sources` | ✅ | ❌ | 근거 없으면 `null`이 아니라 빈 배열 `[]` |
| `guide_card` | ✅ | ❌ | 항상 존재 (내부 필드는 위와 동일하게 값 없으면 `null`) |

---

## 3. 타임아웃 & 재시도 정책

| 항목 | 값 | 비고 |
|---|---|---|
| Connect timeout | 3s | |
| Read timeout | 30s | RAG 검색 + Gemini 추론 포함이라 단순 조회보다 넉넉하게 잡음 |
| 재시도 대상 | 5xx, 타임아웃 | **1회만** 재시도 |
| 재시도 후에도 실패 | Spring이 판정을 `FAILED`로 확정 처리 | 무한 재시도 없음 |
| 4xx | 재시도 없음 | Python이 원천적으로 처리 못 하는 입력 → 재시도해도 결과 동일 |

**알려진 한계 (의도적 스코프아웃)**: 타임아웃 후 재시도 시 `Idempotency-Key`가 없어, Python이 실제로는 응답만 유실됐을 뿐 판정을 이미 끝낸 경우 Gemini 호출이 중복될 수 있다. 이 API는 Python이 DB에 아무것도 쓰지 않는 stateless 구조(읽기+Gemini 호출만)라 데이터 정합성이 깨지진 않고, 최악의 경우도 Gemini 비용 중복 정도라 MVP 범위에서는 허용한다. 429도 별도 처리 없이 일반 4xx로 묶어 재시도하지 않는다(안전한 기본 동작). 요청량이 늘어나 중복 호출 비용이 유의미해지면 그때 Idempotency-Key 기반 중복 제거를 추가한다.

---

## 4. 에러 매핑 (Python 응답 → Spring 처리)

| Python 응답 | Spring 판정 | 클라이언트에 노출되는 값 |
|---|---|---|
| 200 | 정상 처리, `COMPLETED` | 판정 결과 전체 |
| 4xx | 재시도 없이 즉시 실패 | `errorCode: JUDGMENT_005` |
| 5xx / timeout (1회 재시도 후에도 실패) | 실패 | `errorCode: JUDGMENT_005` |

`JUDGMENT_005`(AI 서비스 실패)는 HTTP 레벨 에러가 아니라, `GET /api/judgments/{id}` 응답이 `200 OK`인 채로 `data.status: "FAILED"` 안에 담겨 나간다. 기존 401/422/429 같은 `ApiResponse.error` 패턴과 혼동하지 않는다. 실패 시 해당 요청이 소모한 일일 판정 횟수는 환불한다 (원인 구분 없이 `FAILED`면 전부 환불).

**로깅은 구분한다.** 사용자에게 보여주는 값(`JUDGMENT_005`)은 원인 불문 동일하지만, Spring 서버 로그는 원인별로 다르게 남긴다:
- Python이 `401`(잘못된/누락된 `X-Internal-Api-Key`)을 반환하면 → **개별 요청 실패가 아니라 배포 설정 오류**다. `log.error`로 크게 남겨서 눈에 띄게 한다 — 방치하면 모든 판정 요청이 조용히 다 실패하면서 사용자만 계속 환불받는 상태가 될 수 있다.
- 그 외 4xx/5xx/timeout은 개별 요청 단위 실패로 보고 `log.warn` 수준으로 남긴다.

---

## 5. RAG / 아카이브 데이터 소유권

Python이 자체 벡터스토어(Chroma, 로컬 persistent client)를 소유한다. `archive_items`는 MySQL(Spring 쪽)에 원본이 있지만, 실시간 판정 요청 경로에서 Python이 MySQL에 접근하는 일은 없다.

- **실시간 경로**: Spring → Python(`/internal/api/v1/judgments`) → 이미 만들어진 벡터스토어에서 검색만. DB 접근 없음.
- **인제스트(배치, 오프라인)**: 아카이브 등록/수정은 관리자가 MySQL에 직접 반영하는 구조이므로(관리자 API 없음), Python의 인제스트 스크립트가 이때만 예외적으로 MySQL을 읽기전용으로 조회해 벡터스토어를 갱신한다. 라이브 서빙 경로와 분리된 배치 작업이라는 전제하에 허용한다.

---

## 6. Python 내부 구조 원칙

이번 프로젝트는 Python 쪽 도메인이 "판정" 하나뿐이라, Port/Adapter 같은 인터페이스 계층은 두지 않는다. 대신 역할만 분리한다.

```text
app/
├── llm/            # 공용 연결 팩토리 — 키/타임아웃/모델명만, 계약 없음
├── judgment/        # 도메인 로직 — 프롬프트, RAG 오케스트레이션, 응답 파싱
│   └── rag/          # 벡터스토어, 리트리버, 인제스트
```

도메인이 두 번째로 생기거나 LLM provider를 교체해야 할 상황이 오면 그때 Port/Adapter로 승격한다 (지금 미리 만들 이유 없음).

---

## 최종 체크리스트

- [ ] Spring은 Python 호출 전 입력을 항상 순수 텍스트로 정규화한다 (OCR/메타데이터 추출은 Spring 책임)
- [ ] Python 응답은 snake_case, `trust_level`은 Spring `TrustLevel` enum과 1:1 매핑
- [ ] 한글 라벨(`trustLevelLabel`, 배지 라벨)은 Spring이 붙인다 — Python 응답에 포함하지 않는다
- [ ] Connect 3s / Read 30s, 5xx·timeout만 1회 재시도, 4xx는 재시도 없음
- [ ] 재시도 소진 시 `JUDGMENT_005`로 매핑하고 판정을 `FAILED` 처리, 일일 횟수 환불
- [ ] Python은 실시간 요청 경로에서 MySQL에 접근하지 않는다 (인제스트만 예외, 오프라인 배치)
- [ ] `X-Internal-Api-Key` 헤더 없이 호출되는 요청은 거부한다
