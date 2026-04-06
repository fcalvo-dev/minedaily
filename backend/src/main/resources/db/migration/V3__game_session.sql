CREATE TABLE IF NOT EXISTS game_session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(255) NOT NULL,
    daily_challenge_id BIGINT NOT NULL REFERENCES daily_challenge(id),
    status VARCHAR(16) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE NULL,
    max_lives INTEGER NOT NULL,
    remaining_lives INTEGER NOT NULL,
    error_count INTEGER NOT NULL,
    click_count INTEGER NOT NULL,
    board_snapshot TEXT NOT NULL,
    active_session_key VARCHAR(512) NULL UNIQUE,
    CONSTRAINT chk_game_session_status CHECK (status IN ('IN_PROGRESS', 'WON', 'LOST')),
    CONSTRAINT chk_game_session_max_lives_positive CHECK (max_lives > 0),
    CONSTRAINT chk_game_session_remaining_lives_range CHECK (remaining_lives >= 0 AND remaining_lives <= max_lives),
    CONSTRAINT chk_game_session_error_count_non_negative CHECK (error_count >= 0),
    CONSTRAINT chk_game_session_click_count_non_negative CHECK (click_count >= 0),
    CONSTRAINT chk_game_session_ended_at_consistency CHECK (
        (status = 'IN_PROGRESS' AND ended_at IS NULL) OR
        (status IN ('WON', 'LOST') AND ended_at IS NOT NULL)
    ),
    CONSTRAINT chk_game_session_active_key_consistency CHECK (
        (status = 'IN_PROGRESS' AND active_session_key IS NOT NULL) OR
        (status IN ('WON', 'LOST') AND active_session_key IS NULL)
    )
);

CREATE INDEX idx_game_session_user_challenge_status
    ON game_session (user_id, daily_challenge_id, status);
