CREATE TABLE briefing_views (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                daily_briefing_id BIGINT NOT NULL,
                                opened_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                FOREIGN KEY (daily_briefing_id) REFERENCES daily_briefings(id) ON DELETE CASCADE,
                                UNIQUE KEY uq_briefing_views_user_briefing (user_id, daily_briefing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;