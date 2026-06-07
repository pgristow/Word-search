-- V9: user_progress.user_id was never given a UNIQUE constraint, so duplicate rows
-- crept in and findByUserId started failing ("did not return a unique result").
-- Keep the highest-progress row per user, delete the rest, then enforce uniqueness.
-- Uses a window function (supported by both PostgreSQL and H2).
DELETE FROM user_progress
WHERE id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY total_score DESC, id) AS rn
        FROM user_progress
    ) ranked
    WHERE ranked.rn > 1
);

ALTER TABLE user_progress ADD CONSTRAINT uq_user_progress_user_id UNIQUE (user_id);
