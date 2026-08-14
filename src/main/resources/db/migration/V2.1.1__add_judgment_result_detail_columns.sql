-- FAILED 상태 및 판정 결과 상세 필드 지원을 위한 컬럼 추가.
-- (docs/AI_SERVICE_CONTRACT.md, Judgment API 명세서 확정 이후 발견된 스키마 갭)
ALTER TABLE judgments
    ADD COLUMN failure_error_code VARCHAR(30) AFTER status,
    ADD COLUMN safety_notice TEXT AFTER evidence_summary,
    ADD COLUMN sources_json JSON AFTER guide_card_json,
    ADD COLUMN conflict_description TEXT AFTER conflict_type;
