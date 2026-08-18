-- 아카이브 승격 후보 파악용 조회 (growth loop 1단계).
-- 완전 자동 반영 아님 — 관리자가 이 결과를 보고 "이건 아카이브로 정식 만들자" 판단하는 참고 자료.
-- 골라낸 후보는 AACtion_AI 저장소의 app/judgment/rag/content/archive_seed.csv에 행 추가 →
-- sync_archive_csv.py 실행 → 반영.

-- v1: 완료된 판정을 원문 기준으로 묶어서, 많이 물어본 순으로.
-- (완전히 같은 문장일 때만 묶임 — "다르게 물어봤지만 같은 주제"는 아직 못 잡는다. 관리자가 눈으로 걸러야 함.
--  대소문자/공백 차이 정도는 오히려 같은 후보로 묶이는 게 이 용도엔 도움이 돼서 굳이 바이트 단위로 안 쪼갠다.)
SELECT
    input_text,
    category_id,
    COUNT(*)                                        AS ask_count,
    GROUP_CONCAT(DISTINCT COALESCE(trust_level, 'UNKNOWN')) AS trust_levels_seen,
    MAX(created_at)                                 AS last_asked_at
FROM judgments
WHERE status = 'COMPLETED'
  AND input_text IS NOT NULL
  AND input_text REGEXP '[^[:space:]]'  -- TRIM은 스페이스만 지우고 탭/개행은 안 지워서 REGEXP로 검사
  AND category_id IS NOT NULL  -- ArchiveItem.categoryId는 필수라 없으면 그대로 승격 못 함
GROUP BY input_text, category_id
HAVING ask_count >= 2
ORDER BY ask_count DESC, last_asked_at DESC
LIMIT 30;

-- TODO: judgments.archive_item_id가 지금 항상 NULL임 (Python이 어떤 archive_item을
-- 근거로 썼는지 아직 응답에 안 담아줌 — task #13, JudgmentService.completeJudgment 참고).
-- 그 값이 실제로 채워지기 전까진 아래 필터를 넣어도 검증할 방법이 없어서 지금은 보류.
-- 채워지고 나면 이렇게 그룹 단위로 "이미 아카이브로 완전히 커버된 주제"만 제외한다
-- (그룹 안에 아카이브 안 된 행이 하나라도 있으면 후보로 남겨야 하므로 단순 WHERE로는 부족함):
--   HAVING ask_count >= 2 AND COUNT(archive_item_id) = 0
