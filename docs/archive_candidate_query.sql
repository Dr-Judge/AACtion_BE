-- 아카이브 승격 후보 파악용 조회 (growth loop 1단계).
-- 완전 자동 반영 아님 — 관리자가 이 결과를 보고 "이건 아카이브로 정식 만들자" 판단하는 참고 자료.
-- 골라낸 후보는 docs/archive_seed.csv에 행 추가 → scripts/csv_to_archive_sql.py로 SQL 생성 → 반영.

-- v1: 완료된 판정을 원문 기준으로 묶어서, 많이 물어본 순으로.
-- (완전히 같은 문장일 때만 묶임 — "다르게 물어봤지만 같은 주제"는 아직 못 잡는다. 관리자가 눈으로 걸러야 함)
SELECT
    input_text,
    category_id,
    COUNT(*)                           AS ask_count,
    GROUP_CONCAT(DISTINCT trust_level) AS trust_levels_seen,
    MAX(created_at)                    AS last_asked_at
FROM judgments
WHERE status = 'COMPLETED'
GROUP BY input_text, category_id
HAVING ask_count >= 2
ORDER BY ask_count DESC, last_asked_at DESC
LIMIT 30;

-- TODO: judgments.archive_item_id가 지금 항상 NULL임 (Python이 어떤 archive_item을
-- 근거로 썼는지 아직 응답에 안 담아줌 — JudgmentService.completeJudgment 참고).
-- 이게 채워지면 위 쿼리에 `WHERE archive_item_id IS NULL`을 추가해서
-- "이미 아카이브로 커버된 주제"는 후보에서 자동으로 빼줄 수 있다.
