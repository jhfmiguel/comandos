ALTER TABLE IF EXISTS erp_inventory_transfer
    ADD COLUMN IF NOT EXISTS rejected_by_id BIGINT;

ALTER TABLE IF EXISTS erp_inventory_transfer
    ADD COLUMN IF NOT EXISTS rejected_by_login VARCHAR(255);

ALTER TABLE IF EXISTS erp_inventory_transfer
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP;

ALTER TABLE IF EXISTS erp_inventory_transfer
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(1000);
