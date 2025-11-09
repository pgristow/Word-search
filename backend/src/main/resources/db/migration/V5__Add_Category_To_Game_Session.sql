-- Add category_id to game_sessions table
ALTER TABLE game_sessions
ADD COLUMN category_id UUID;

-- Add foreign key constraint
ALTER TABLE game_sessions
ADD CONSTRAINT fk_game_sessions_category
FOREIGN KEY (category_id) REFERENCES categories(id);
