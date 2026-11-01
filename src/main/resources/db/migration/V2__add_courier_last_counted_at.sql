ALTER TABLE courier ADD COLUMN last_counted_at TIMESTAMP WITH TIME ZONE;
UPDATE courier SET last_counted_at = last_recorded_at;
ALTER TABLE courier ALTER COLUMN last_counted_at SET NOT NULL;