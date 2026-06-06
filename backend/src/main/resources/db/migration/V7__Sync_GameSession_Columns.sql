-- V7: add GameSession columns that exist on the JPA entity but were never created by an
-- earlier migration. On a fresh database (e.g. a new Render Postgres) Hibernate's
-- schema validation fails without these. IF NOT EXISTS keeps it safe on databases that
-- already auto-created the columns.
ALTER TABLE game_sessions ADD COLUMN IF NOT EXISTS current_combo INTEGER DEFAULT 0;
ALTER TABLE game_sessions ADD COLUMN IF NOT EXISTS last_word_found_at TIMESTAMP;
