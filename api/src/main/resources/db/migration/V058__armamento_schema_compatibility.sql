-- COMANDOS
-- Migration 058
-- Compatibilidade do schema existente com o modulo Armamento.
--
-- Estrategia:
-- 1. adiciona campos ausentes inicialmente permitindo NULL;
-- 2. preenche registros preexistentes;
-- 3. aplica defaults quando fazem parte do dominio;
-- 4. aplica NOT NULL somente depois do backfill;
-- 5. corrige tipos fisicos incompatíveis com o mapeamento JPA.
--
-- UNSPECIFIED identifica registros historicos de municao criados antes
-- da existencia do campo ammunition_type. Novos cadastros continuam
-- obrigados pela aplicacao a informar o tipo real da municao.

BEGIN;

-- ============================================================
-- Local de estoque
-- StockLocation.active = true
-- ============================================================

ALTER TABLE erp_stock_location
    ADD COLUMN IF NOT EXISTS active boolean;

UPDATE erp_stock_location
SET active = true
WHERE active IS NULL;

ALTER TABLE erp_stock_location
    ALTER COLUMN active SET DEFAULT true,
    ALTER COLUMN active SET NOT NULL;


-- ============================================================
-- Lote de estoque
-- StockLot.condition = GOOD
-- StockLot.status = AVAILABLE
-- ============================================================

ALTER TABLE erp_stock_lot
    ADD COLUMN IF NOT EXISTS condition varchar(100);

ALTER TABLE erp_stock_lot
    ADD COLUMN IF NOT EXISTS status varchar(100);

UPDATE erp_stock_lot
SET condition = 'GOOD'
WHERE condition IS NULL;

UPDATE erp_stock_lot
SET status = 'AVAILABLE'
WHERE status IS NULL;

ALTER TABLE erp_stock_lot
    ALTER COLUMN condition SET DEFAULT 'GOOD',
    ALTER COLUMN condition SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'AVAILABLE',
    ALTER COLUMN status SET NOT NULL;


-- ============================================================
-- Ordem de manutencao
-- WorkOrder.maintenanceType = CORRECTIVE
-- ============================================================

ALTER TABLE erp_work_order
    ADD COLUMN IF NOT EXISTS maintenance_type varchar(30);

UPDATE erp_work_order
SET maintenance_type = 'CORRECTIVE'
WHERE maintenance_type IS NULL;

ALTER TABLE erp_work_order
    ALTER COLUMN maintenance_type SET DEFAULT 'CORRECTIVE',
    ALTER COLUMN maintenance_type SET NOT NULL;


-- ============================================================
-- Especificacao de municao
--
-- Registros historicos nao possuem informacao suficiente para
-- reconstruir ammunition_type com seguranca.
-- UNSPECIFIED evita fabricar classificacao inexistente.
-- ============================================================

ALTER TABLE erp_ammunition_specification
    ADD COLUMN IF NOT EXISTS ammunition_type varchar(100);

UPDATE erp_ammunition_specification
SET ammunition_type = 'UNSPECIFIED'
WHERE ammunition_type IS NULL;

ALTER TABLE erp_ammunition_specification
    ALTER COLUMN ammunition_type SET NOT NULL;


-- ============================================================
-- Anexos operacionais
--
-- O modelo Java utiliza byte[] / PostgreSQL bytea.
-- A tabela existente utiliza oid.
--
-- A conversao automatica oid -> bytea nao e segura.
-- Esta migration somente executa a alteracao quando nao existem
-- anexos armazenados.
-- ============================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'erp_operation_attachment'
          AND column_name = 'content'
          AND data_type = 'oid'
    ) THEN

        IF EXISTS (
            SELECT 1
            FROM erp_operation_attachment
        ) THEN
            RAISE EXCEPTION
                'Migration 058: erp_operation_attachment possui dados; conversao oid -> bytea exige migracao especifica.';
        END IF;

        ALTER TABLE erp_operation_attachment
            ALTER COLUMN content DROP NOT NULL;

        ALTER TABLE erp_operation_attachment
            DROP COLUMN content;

        ALTER TABLE erp_operation_attachment
            ADD COLUMN content bytea NOT NULL;
    END IF;
END
$$;

COMMIT;