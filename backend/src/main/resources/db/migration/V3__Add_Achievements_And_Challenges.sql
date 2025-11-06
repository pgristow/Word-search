-- Daily Challenges Table
CREATE TABLE daily_challenges (
    id UUID PRIMARY KEY,
    challenge_date DATE NOT NULL UNIQUE,
    category_id UUID REFERENCES categories(id),
    difficulty_level INTEGER DEFAULT 1,
    target_score INTEGER,
    target_words INTEGER,
    time_limit_seconds INTEGER,
    bonus_multiplier FLOAT DEFAULT 1.5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_challenges_date ON daily_challenges(challenge_date);

-- User Daily Challenge Attempts Table
CREATE TABLE user_daily_attempts (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    challenge_id UUID REFERENCES daily_challenges(id),
    score_achieved INTEGER DEFAULT 0,
    words_found INTEGER DEFAULT 0,
    time_taken_seconds INTEGER,
    completed BOOLEAN DEFAULT false,
    reward_claimed BOOLEAN DEFAULT false,
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, challenge_id)
);

CREATE INDEX idx_user_daily_attempts ON user_daily_attempts(user_id, challenge_id);

-- Achievements Table
CREATE TABLE achievements (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    category VARCHAR(50), -- 'SCORE', 'WORDS', 'BOSS', 'STREAK', 'SPEED'
    requirement_type VARCHAR(50), -- 'TOTAL_SCORE', 'WORDS_FOUND', 'BOSS_DEFEATED', etc.
    requirement_value INTEGER,
    icon_url VARCHAR(255),
    reward_points INTEGER DEFAULT 100,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Achievements Table
CREATE TABLE user_achievements (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    achievement_id UUID REFERENCES achievements(id),
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    progress INTEGER DEFAULT 0,
    completed BOOLEAN DEFAULT false,
    UNIQUE(user_id, achievement_id)
);

CREATE INDEX idx_user_achievements ON user_achievements(user_id, completed);

-- Insert default achievements
INSERT INTO achievements (id, name, description, category, requirement_type, requirement_value, reward_points)
VALUES
    ('a1000000-0000-0000-0000-000000000001', 'First Steps', 'Find your first word', 'WORDS', 'WORDS_FOUND', 1, 50),
    ('a1000000-0000-0000-0000-000000000002', 'Word Hunter', 'Find 100 words', 'WORDS', 'WORDS_FOUND', 100, 200),
    ('a1000000-0000-0000-0000-000000000003', 'Word Master', 'Find 1000 words', 'WORDS', 'WORDS_FOUND', 1000, 500),
    ('a1000000-0000-0000-0000-000000000004', 'Score Rookie', 'Reach 10,000 total score', 'SCORE', 'TOTAL_SCORE', 10000, 100),
    ('a1000000-0000-0000-0000-000000000005', 'Score Expert', 'Reach 100,000 total score', 'SCORE', 'TOTAL_SCORE', 100000, 300),
    ('a1000000-0000-0000-0000-000000000006', 'Boss Slayer', 'Defeat your first boss', 'BOSS', 'BOSS_DEFEATED', 1, 250),
    ('a1000000-0000-0000-0000-000000000007', 'Boss Destroyer', 'Defeat 10 bosses', 'BOSS', 'BOSS_DEFEATED', 10, 500),
    ('a1000000-0000-0000-0000-000000000008', 'Streak Starter', 'Maintain a 3-day streak', 'STREAK', 'STREAK_DAYS', 3, 150),
    ('a1000000-0000-0000-0000-000000000009', 'Dedicated Player', 'Maintain a 7-day streak', 'STREAK', 'STREAK_DAYS', 7, 300),
    ('a1000000-0000-0000-0000-00000000000a', 'Reverse Expert', 'Find 50 reversed words', 'WORDS', 'REVERSED_WORDS', 50, 200),
    ('a1000000-0000-0000-0000-00000000000b', 'Combo King', 'Achieve a 10x combo', 'SCORE', 'HIGHEST_COMBO', 10, 400),
    ('a1000000-0000-0000-0000-00000000000c', 'Speed Demon', 'Complete a level in under 60 seconds', 'SPEED', 'FAST_COMPLETION', 1, 250),
    ('a1000000-0000-0000-0000-00000000000d', 'Level 10', 'Reach level 10', 'LEVEL', 'CURRENT_LEVEL', 10, 150),
    ('a1000000-0000-0000-0000-00000000000e', 'Level 25', 'Reach level 25', 'LEVEL', 'CURRENT_LEVEL', 25, 300),
    ('a1000000-0000-0000-0000-00000000000f', 'Level 50', 'Reach level 50', 'LEVEL', 'CURRENT_LEVEL', 50, 1000);
