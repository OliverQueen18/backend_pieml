ALTER TABLE centers ADD COLUMN IF NOT EXISTS opening_time TIME;
ALTER TABLE centers ADD COLUMN IF NOT EXISTS closing_time TIME;
ALTER TABLE centers ADD COLUMN IF NOT EXISTS processing_delay_days INTEGER;

UPDATE centers SET opening_time = TIME '08:00:00' WHERE opening_time IS NULL;
UPDATE centers SET closing_time = TIME '17:00:00' WHERE closing_time IS NULL;
UPDATE centers SET processing_delay_days = 3 WHERE processing_delay_days IS NULL;

CREATE TABLE IF NOT EXISTS center_opening_days (
    center_id BIGINT NOT NULL REFERENCES centers(id),
    day_of_week VARCHAR(16) NOT NULL,
    PRIMARY KEY (center_id, day_of_week)
);

INSERT INTO center_opening_days (center_id, day_of_week)
SELECT c.id, d.day
FROM centers c
CROSS JOIN (VALUES
    ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')
) AS d(day)
WHERE NOT EXISTS (
    SELECT 1 FROM center_opening_days cod WHERE cod.center_id = c.id
);
