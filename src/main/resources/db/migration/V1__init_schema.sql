-- ============================================
-- 닥터저지 V1 baseline (누구에게도 배정하지 않음 — DB_MIGRATION_RULES.md 참고)
-- 이후 신규 변경은 담당자별 레인(V2.x.y ~ V4.x.y)에서만 증분으로 추가한다.
-- ============================================

CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(30) UNIQUE NOT NULL,
    name VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    kakao_id VARCHAR(50) UNIQUE NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    age_group VARCHAR(20),
    gender VARCHAR(10),
    point_balance INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_interests (
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, category_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE archive_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target VARCHAR(200) NOT NULL,
    effect VARCHAR(200) NOT NULL,
    condition_scope VARCHAR(300),
    category_id BIGINT NOT NULL,
    trust_level VARCHAR(30) NOT NULL,
    evidence_source_type VARCHAR(50),
    evidence_sources_json JSON,
    evidence_summary TEXT NOT NULL,
    embedding_vector JSON,
    version VARCHAR(30),
    managed_by VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE judgments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    input_type VARCHAR(10) NOT NULL,
    input_text TEXT,
    category_id BIGINT,
    trust_level VARCHAR(30),
    conflict_detected BOOLEAN NOT NULL DEFAULT FALSE,
    conflict_type VARCHAR(30),
    evidence_summary TEXT,
    guide_card_json JSON,
    archive_item_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (archive_item_id) REFERENCES archive_items(id) ON DELETE SET NULL,
    INDEX idx_judgments_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE feed_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    judgment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    like_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (judgment_id) REFERENCES judgments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_feed_posts_sort (is_public, created_at, like_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE feed_likes (
    feed_post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (feed_post_id, user_id),
    FOREIGN KEY (feed_post_id) REFERENCES feed_posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE share_links (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    judgment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    token VARCHAR(32) UNIQUE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (judgment_id) REFERENCES judgments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE points_ledger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    reason VARCHAR(30) NOT NULL,
    amount INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attendance_checks (
    user_id BIGINT NOT NULL,
    checked_date DATE NOT NULL,
    PRIMARY KEY (user_id, checked_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 일일 판정 요청 한도 카운터 (Redis 대신 MySQL로 — R-7YUWXQ)
CREATE TABLE judgment_request_counts (
    user_id BIGINT NOT NULL,
    request_date DATE NOT NULL,
    request_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, request_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_briefings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    briefing_date DATE NOT NULL,
    archive_item_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (archive_item_id) REFERENCES archive_items(id),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_briefings_date (briefing_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 관심 카테고리 5개 (온보딩 탭 옵션) — 기준값이라 baseline에 포함
INSERT INTO categories (code, name, display_order) VALUES
  ('NUTRITION', '영양제·건강기능식품', 1),
  ('DIET', '다이어트·체중관리', 2),
  ('DISEASE_CARE', '질환·증상관리', 3),
  ('COSMETICS', '화장품', 4),
  ('PROCEDURE_SKINCARE', '시술·피부관리', 5);
