-- 판정 이력/공유 카드에 보여줄 의문문 형태 요약 제목.
-- AI 판정 응답(title)을 그대로 저장한다. 기존 행은 채울 방법이 없어 NULL 허용.
ALTER TABLE judgments
    ADD COLUMN title VARCHAR(255) AFTER input_text;
