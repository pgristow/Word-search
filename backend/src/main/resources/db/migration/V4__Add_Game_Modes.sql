-- Add game mode support to game sessions
ALTER TABLE game_sessions
ADD COLUMN IF NOT EXISTS game_mode VARCHAR(20) DEFAULT 'CLASSIC';

-- Add pause state for casual mode save/resume
ALTER TABLE game_sessions
ADD COLUMN IF NOT EXISTS is_paused BOOLEAN DEFAULT false;

-- Create index for querying by game mode
CREATE INDEX IF NOT EXISTS idx_game_sessions_mode ON game_sessions(game_mode);

-- Add game mode tracking to user progress
ALTER TABLE user_progress
ADD COLUMN IF NOT EXISTS casual_puzzles_completed INT DEFAULT 0;

-- Comment for documentation
COMMENT ON COLUMN game_sessions.game_mode IS 'Game mode: CLASSIC (competitive with timers) or CASUAL (relaxed, no timers)';
COMMENT ON COLUMN game_sessions.is_paused IS 'Whether the game is paused (used for casual mode save/resume functionality)';
COMMENT ON COLUMN user_progress.casual_puzzles_completed IS 'Number of puzzles completed in casual mode';
