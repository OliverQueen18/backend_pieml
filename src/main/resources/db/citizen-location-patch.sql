ALTER TABLE citizens ADD COLUMN IF NOT EXISTS address VARCHAR(500);
ALTER TABLE citizens ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE citizens ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

UPDATE citizens
SET address = 'Bamako, Mali'
WHERE address IS NULL OR TRIM(address) = '';

UPDATE citizens
SET latitude = 12.6392
WHERE latitude IS NULL;

UPDATE citizens
SET longitude = -8.0029
WHERE longitude IS NULL;
