-- =============================================================
-- V5__loan_product_support.sql
-- Adds Loan Product master + Product-Module mapping, and extends
-- api_scenario_mapping with an optional product_id so a single
-- (scenario, api) pair can resolve to different sequences per product.
-- Backward compatible: existing rows have product_id = NULL and continue
-- to drive the 2-arg journeyExecutor.execute(module, scenario) flow.
-- =============================================================

-- ---------------------------------------------------------------
-- 1. loan_product_master
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS loan_product_master (
    id              BIGSERIAL       PRIMARY KEY,
    product_code    VARCHAR(50)     NOT NULL UNIQUE,
    product_name    VARCHAR(255)    NOT NULL,
    description     TEXT,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_product_active ON loan_product_master(active);

-- ---------------------------------------------------------------
-- 2. product_module_mapping (which LOS modules are valid for a product)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_module_mapping (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES loan_product_master(id) ON DELETE CASCADE,
    module_code     VARCHAR(100)    NOT NULL,
    module_name     VARCHAR(255),
    sequence_no     INTEGER         NOT NULL DEFAULT 0,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    UNIQUE(product_id, module_code)
);

CREATE INDEX IF NOT EXISTS idx_product_module_lookup ON product_module_mapping(product_id, module_code, active);

-- ---------------------------------------------------------------
-- 3. Extend api_scenario_mapping with optional product_id
--    Replace UNIQUE(scenario_id, api_id) -> UNIQUE(scenario_id, api_id, product_id)
--    so the same API can be mapped to one scenario across different products.
-- ---------------------------------------------------------------
ALTER TABLE api_scenario_mapping
    ADD COLUMN IF NOT EXISTS product_id BIGINT REFERENCES loan_product_master(id);

-- The original V3 migration declared UNIQUE(scenario_id, api_id) inline,
-- so Postgres auto-named it. Drop it defensively and rebuild.
DO $$
DECLARE
    cname text;
BEGIN
    SELECT conname INTO cname
    FROM pg_constraint
    WHERE conrelid = 'api_scenario_mapping'::regclass
      AND contype = 'u'
      AND pg_get_constraintdef(oid) ILIKE 'UNIQUE (scenario_id, api_id)';
    IF cname IS NOT NULL THEN
        EXECUTE format('ALTER TABLE api_scenario_mapping DROP CONSTRAINT %I', cname);
    END IF;
END $$;

ALTER TABLE api_scenario_mapping
    ADD CONSTRAINT uq_scenario_api_product UNIQUE (scenario_id, api_id, product_id);

CREATE INDEX IF NOT EXISTS idx_mapping_product ON api_scenario_mapping(product_id);

-- ---------------------------------------------------------------
-- 4. Capture the loan product on each journey step
-- ---------------------------------------------------------------
ALTER TABLE journey_execution_history
    ADD COLUMN IF NOT EXISTS loan_product_code VARCHAR(50);
