-- 일일 한도 예약 시점의 날짜를 판정 레코드에 저장해둔다.
-- OCR/유튜브 추출이 자정을 걸쳐 처리되면 created_at(INSERT 시각)의 날짜가
-- 예약 때 실제로 차감한 날짜와 달라질 수 있어, processAsync 실패 시 잘못된
-- 날짜로 환불하는 버그가 있었다 (JudgmentService.failJudgment).
ALTER TABLE judgments
    ADD COLUMN request_date DATE AFTER user_id;

UPDATE judgments
SET request_date = DATE(created_at)
WHERE request_date IS NULL;

ALTER TABLE judgments
    MODIFY COLUMN request_date DATE NOT NULL;
