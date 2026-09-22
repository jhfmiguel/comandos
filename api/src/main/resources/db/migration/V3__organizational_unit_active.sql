ALTER TABLE IF EXISTS erp_organizational_unit
    ADD COLUMN IF NOT EXISTS active BOOLEAN;

UPDATE erp_organizational_unit
SET active = TRUE
WHERE active IS NULL;

ALTER TABLE IF EXISTS erp_organizational_unit
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE IF EXISTS erp_organizational_unit
    ALTER COLUMN active SET NOT NULL;
