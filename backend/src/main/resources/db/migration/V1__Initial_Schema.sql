-- Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    total_words_found INTEGER DEFAULT 0,
    total_puzzles_completed INTEGER DEFAULT 0,
    current_level INTEGER DEFAULT 1,
    is_premium BOOLEAN DEFAULT false,
    premium_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Categories Table
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url VARCHAR(255),
    unlock_requirement_type VARCHAR(50),
    unlock_requirement_value INTEGER,
    display_order INTEGER,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Words Table (word bank per category)
CREATE TABLE words (
    id UUID PRIMARY KEY,
    category_id UUID REFERENCES categories(id),
    word VARCHAR(50) NOT NULL,
    difficulty_level INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_words_category ON words(category_id);
CREATE INDEX idx_words_difficulty ON words(category_id, difficulty_level);

-- User Progress Table (Endless Mode)
CREATE TABLE user_progress (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    current_level INTEGER DEFAULT 1,
    total_score BIGINT DEFAULT 0,
    current_session_score INTEGER DEFAULT 0,
    highest_level_reached INTEGER DEFAULT 1,
    highest_combo INTEGER DEFAULT 0,
    total_words_found INTEGER DEFAULT 0,
    total_reversed_words_found INTEGER DEFAULT 0,
    last_played_at TIMESTAMP,
    current_streak_days INTEGER DEFAULT 0,
    longest_streak_days INTEGER DEFAULT 0,
    last_streak_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);

-- Game Sessions Table
CREATE TABLE game_sessions (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    session_start TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_end TIMESTAMP,
    starting_level INTEGER,
    ending_level INTEGER,
    total_score INTEGER DEFAULT 0,
    words_found INTEGER DEFAULT 0,
    highest_combo INTEGER DEFAULT 0,
    boss_levels_completed INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true
);

CREATE INDEX idx_active_sessions ON game_sessions(user_id, is_active);

-- Boss Level Attempts Table
CREATE TABLE boss_level_attempts (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    session_id UUID REFERENCES game_sessions(id),
    boss_level INTEGER NOT NULL,
    boss_type VARCHAR(50),
    words_required INTEGER,
    words_found INTEGER,
    time_limit_seconds INTEGER,
    time_taken_seconds INTEGER,
    shuffles_occurred INTEGER DEFAULT 0,
    completed BOOLEAN DEFAULT false,
    score_earned INTEGER DEFAULT 0,
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Found Words Table
CREATE TABLE user_found_words (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    session_id UUID REFERENCES game_sessions(id),
    word VARCHAR(50) NOT NULL,
    is_reversed BOOLEAN DEFAULT false,
    score_earned INTEGER,
    found_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_found_words ON user_found_words(user_id, session_id);

-- Leaderboard Table
CREATE TABLE leaderboard (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    total_score BIGINT DEFAULT 0,
    highest_level INTEGER DEFAULT 1,
    rank INTEGER,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);

CREATE INDEX idx_leaderboard_rank ON leaderboard(rank);

-- Premium Subscriptions Table
CREATE TABLE premium_subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    is_premium BOOLEAN DEFAULT false,
    purchase_date TIMESTAMP,
    expiry_date TIMESTAMP,
    purchase_platform VARCHAR(50),
    purchase_token VARCHAR(255),
    auto_renew BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);

-- Ad Views Table
CREATE TABLE ad_views (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ad_type VARCHAR(50),
    ad_unit_id VARCHAR(100),
    session_id VARCHAR(100)
);

CREATE INDEX idx_ad_views_user_time ON ad_views(user_id, viewed_at);

-- Ad Session State Table
CREATE TABLE ad_session_state (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    session_start_time TIMESTAMP NOT NULL,
    ads_watched_count INTEGER DEFAULT 0,
    ads_remaining INTEGER DEFAULT 5,
    session_expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, session_start_time)
);
