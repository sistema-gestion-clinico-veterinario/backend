-- Add orden column to vistas table with default value 0
ALTER TABLE vistas ADD COLUMN IF NOT EXISTS orden INTEGER DEFAULT 0;

-- Update existing null values to 0
UPDATE vistas SET orden = 0 WHERE orden IS NULL;

-- Add NOT NULL constraint
ALTER TABLE vistas ALTER COLUMN orden SET NOT NULL;
