-- V6: word recognition, scoring, coin economy, leagues, casual, unified leaderboard
-- Uses TEXT (not JSONB) for JSON payloads to stay H2- and Postgres-compatible,
-- matching the existing migration style (V1/V3 use TEXT).

-- Board persistence for server-authoritative path validation + resume
ALTER TABLE game_sessions ADD COLUMN board_state TEXT;

-- Found-word metadata (bonus words, length, traced path)
ALTER TABLE user_found_words ADD COLUMN is_bonus BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE user_found_words ADD COLUMN word_length INT NOT NULL DEFAULT 0;
ALTER TABLE user_found_words ADD COLUMN path TEXT;

-- Economy + progression accumulators on user_progress
ALTER TABLE user_progress ADD COLUMN coins BIGINT NOT NULL DEFAULT 0;
ALTER TABLE user_progress ADD COLUMN total_bonus_words_found INT NOT NULL DEFAULT 0;
ALTER TABLE user_progress ADD COLUMN longest_word_found INT NOT NULL DEFAULT 0;
ALTER TABLE user_progress ADD COLUMN weekly_score BIGINT NOT NULL DEFAULT 0;
ALTER TABLE user_progress ADD COLUMN casual_best_score BIGINT NOT NULL DEFAULT 0;

-- Coin unlock cost for categories
ALTER TABLE categories ADD COLUMN coin_unlock_cost INT NOT NULL DEFAULT 0;

-- Coin ledger
CREATE TABLE coin_transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    delta BIGINT NOT NULL,
    reason VARCHAR(40) NOT NULL,
    ref_id UUID,
    balance_after BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_coin_tx_user ON coin_transactions(user_id, created_at);

-- Cosmetic themes
CREATE TABLE themes (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    coin_cost INT NOT NULL,
    asset_key VARCHAR(80) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE user_owned_themes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    theme_id UUID NOT NULL,
    acquired_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_theme UNIQUE (user_id, theme_id)
);

-- Coin-unlocked categories
CREATE TABLE user_unlocked_categories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    method VARCHAR(20) NOT NULL,
    unlocked_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_category UNIQUE (user_id, category_id)
);

-- League tiers (static)
CREATE TABLE league_tiers (
    id INT PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    tier_order INT NOT NULL,
    promote_count INT NOT NULL,
    relegate_count INT NOT NULL,
    promotion_reward INT NOT NULL
);
INSERT INTO league_tiers (id, name, tier_order, promote_count, relegate_count, promotion_reward) VALUES
 (1, 'Bronze',   1, 7, 0, 50),
 (2, 'Silver',   2, 7, 5, 75),
 (3, 'Gold',     3, 7, 5, 100),
 (4, 'Platinum', 4, 7, 5, 150),
 (5, 'Diamond',  5, 7, 5, 200),
 (6, 'Master',   6, 0, 5, 300);

-- Weekly cohorts
CREATE TABLE league_cohorts (
    id UUID PRIMARY KEY,
    tier_id INT NOT NULL,
    week_key VARCHAR(10) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_cohort_tier_week ON league_cohorts(tier_id, week_key, status);

-- Cohort membership + weekly score
CREATE TABLE league_memberships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    cohort_id UUID NOT NULL,
    weekly_score BIGINT NOT NULL DEFAULT 0,
    final_rank INT,
    result VARCHAR(12),
    CONSTRAINT uq_user_cohort UNIQUE (user_id, cohort_id)
);
CREATE INDEX idx_membership_cohort ON league_memberships(cohort_id, weekly_score);

-- Unified leaderboard projection (GLOBAL_CLASSIC | LEAGUE_WEEKLY | CASUAL_BEST)
CREATE TABLE leaderboard_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    username VARCHAR(80) NOT NULL,
    board_type VARCHAR(20) NOT NULL,
    period_key VARCHAR(40) NOT NULL,
    score BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_lb_user_board_period UNIQUE (user_id, board_type, period_key)
);
CREATE INDEX idx_lb_board_period_score ON leaderboard_entries(board_type, period_key, score);
