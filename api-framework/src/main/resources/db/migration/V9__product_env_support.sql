-- =============================================================
-- V9__product_env_support.sql
-- Adds:
--   1. environment_master table (qa2/qa3/qa4 seed)
--   2. api_master.product_id  (nullable FK to loan_product_master)
--      NULL = global/shared API; set = product-scoped
--   3. backfill: jmx_shg_* APIs -> SHG product
-- =============================================================

-- ---------------------------------------------------------------
-- 1. environment_master
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS environment_master (
    id          BIGSERIAL    PRIMARY KEY,
    env_code    VARCHAR(20)  NOT NULL UNIQUE,
    env_name    VARCHAR(100) NOT NULL,
    base_url    VARCHAR(500) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  environment_master           IS 'Target environments selected at run time; overrides api_master.base_url when env is passed to /journey/execute.';
COMMENT ON COLUMN environment_master.env_code  IS 'Short code used in UI/API params (e.g. qa2, qa3, qa4)';
COMMENT ON COLUMN environment_master.base_url  IS 'Scheme + host that prefixes api_master.endpoint at runtime';

INSERT INTO environment_master (env_code, env_name, base_url) VALUES
  ('qa2', 'QA2', 'https://qa2-mfi.novopay.in'),
  ('qa3', 'QA3', 'https://qa3-mfi.novopay.in'),
  ('qa4', 'QA4', 'https://qa4-mfi.novopay.in')
ON CONFLICT (env_code) DO NOTHING;

-- ---------------------------------------------------------------
-- 2. api_master.product_id (nullable)
-- ---------------------------------------------------------------
ALTER TABLE api_master
    ADD COLUMN IF NOT EXISTS product_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_api_master_product'
          AND table_name      = 'api_master'
    ) THEN
        ALTER TABLE api_master
            ADD CONSTRAINT fk_api_master_product
            FOREIGN KEY (product_id) REFERENCES loan_product_master(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_api_master_product_id ON api_master(product_id);

COMMENT ON COLUMN api_master.product_id IS
    'Optional FK to loan_product_master. NULL = shared API visible to all products. Set = product-scoped API.';

-- ---------------------------------------------------------------
-- 3. backfill SHG-prefixed JMX APIs to the SHG product
-- ---------------------------------------------------------------
UPDATE api_master
   SET product_id = lpm.id
  FROM loan_product_master lpm
 WHERE lpm.product_code = 'SHG'
   AND api_master.api_name LIKE 'jmx_shg_%'
   AND api_master.product_id IS NULL;
