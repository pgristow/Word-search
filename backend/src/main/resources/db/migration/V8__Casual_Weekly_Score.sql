-- V8: cumulative casual score for the current week (resets when the week changes).
-- Paired with casual_week_key so the reset is lazy (no scheduled job needed).
ALTER TABLE user_progress ADD COLUMN casual_weekly_score BIGINT NOT NULL DEFAULT 0;
ALTER TABLE user_progress ADD COLUMN casual_week_key VARCHAR(10);
