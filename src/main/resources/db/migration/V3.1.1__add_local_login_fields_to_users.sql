ALTER TABLE users
    MODIFY COLUMN kakao_id VARCHAR(50) NULL,
    ADD COLUMN login_id VARCHAR(50) NULL AFTER kakao_id,
    ADD COLUMN password VARCHAR(255) NULL AFTER login_id,
    ADD COLUMN email VARCHAR(100) NULL AFTER password,
    ADD COLUMN name VARCHAR(50) NULL AFTER email,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER name,
    ADD CONSTRAINT uq_users_login_id UNIQUE (login_id),
    ADD CONSTRAINT uq_users_email UNIQUE (email);