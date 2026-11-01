ALTER TABLE courier ADD COLUMN last_confirmed_at TIMESTAMP WITH TIME ZONE;
UPDATE courier SET last_confirmed_at = last_recorded_at;
ALTER TABLE courier ALTER COLUMN last_confirmed_at SET NOT NULL;