ALTER TABLE users
    ADD COLUMN profile_image_url VARCHAR(500) NULL AFTER kakao_id;

ALTER TABLE users
DROP INDEX kakao_id,
    ADD CONSTRAINT uq_users_kakao_id UNIQUE (kakao_id);