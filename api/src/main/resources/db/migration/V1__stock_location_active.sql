-- COMANDOS database migration baseline.
-- This migration is intentionally idempotent so existing development databases
-- created previously with Hibernate ddl-auto=update can adopt Flyway safely.

ALTER TABLE IF EXISTS erp_stock_location
    ADD COLUMN IF NOT EXISTS active BOOLEAN;

UPDATE erp_stock_location
SET active = TRUE
WHERE active IS NULL;

ALTER TABLE IF EXISTS erp_stock_location
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE IF EXISTS erp_stock_location
    ALTER COLUMN active SET NOT NULL;
