-- =============================================================
-- V3__journey_tables.sql
-- API Journey Automation Framework - scenario / pre-SQL / post-validation / expected-results
-- =============================================================

-- ---------------------------------------------------------------
-- 1. test_scenario_master: business test scenarios per module
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS test_scenario_master (
    id              BIGSERIAL       PRIMARY KEY,
    scenario_code   VARCHAR(100)    NOT NULL UNIQUE,
    scenario_name   VARCHAR(255)    NOT NULL,
    module_code     VARCHAR(100)    NOT NULL,
    description     TEXT,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_scenario_module ON test_scenario_master(module_code, active);

-- ---------------------------------------------------------------
-- 2. api_scenario_mapping: which APIs run for a given scenario, in what order
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_scenario_mapping (
    id              BIGSERIAL       PRIMARY KEY,
    api_id          BIGINT          NOT NULL REFERENCES api_master(api_id) ON DELETE CASCADE,
    scenario_id     BIGINT          NOT NULL REFERENCES test_scenario_master(id) ON DELETE CASCADE,
    execution_order INTEGER         NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    UNIQUE(scenario_id, api_id)
);

CREATE INDEX IF NOT EXISTS idx_mapping_scenario ON api_scenario_mapping(scenario_id, execution_order);

-- ---------------------------------------------------------------
-- 3. api_pre_sql: ordered SQL statements run before each API call
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_pre_sql (
    id              BIGSERIAL       PRIMARY KEY,
    api_id          BIGINT          NOT NULL REFERENCES api_master(api_id) ON DELETE CASCADE,
    scenario_id     BIGINT          NOT NULL REFERENCES test_scenario_master(id) ON DELETE CASCADE,
    sql_order       INTEGER         NOT NULL,
    sql_query       TEXT            NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_pre_sql_lookup ON api_pre_sql(scenario_id, api_id, sql_order);

-- ---------------------------------------------------------------
-- 4. api_post_validation_sql: SQL run after each API call to fetch
--    actual values from DB for comparison against expected results
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_post_validation_sql (
    id              BIGSERIAL       PRIMARY KEY,
    api_id          BIGINT          NOT NULL REFERENCES api_master(api_id) ON DELETE CASCADE,
    scenario_id     BIGINT          NOT NULL REFERENCES test_scenario_master(id) ON DELETE CASCADE,
    validation_name VARCHAR(255)    NOT NULL,
    sql_query       TEXT            NOT NULL,
    expected_column VARCHAR(255)    NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_post_sql_lookup ON api_post_validation_sql(scenario_id, api_id);

-- ---------------------------------------------------------------
-- 5. api_expected_results: expected values per scenario / api / key
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_expected_results (
    id              BIGSERIAL       PRIMARY KEY,
    scenario_id     BIGINT          NOT NULL REFERENCES test_scenario_master(id) ON DELETE CASCADE,
    api_id          BIGINT          NOT NULL REFERENCES api_master(api_id) ON DELETE CASCADE,
    validation_key  VARCHAR(255)    NOT NULL,
    expected_value  TEXT,
    UNIQUE(scenario_id, api_id, validation_key)
);

CREATE INDEX IF NOT EXISTS idx_expected_lookup ON api_expected_results(scenario_id, api_id);

-- ---------------------------------------------------------------
-- 6. journey_execution_history: one row per API step in a journey run
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS journey_execution_history (
    id                  BIGSERIAL       PRIMARY KEY,
    journey_run_id      VARCHAR(64)     NOT NULL,
    module_code         VARCHAR(100)    NOT NULL,
    scenario_code       VARCHAR(100)    NOT NULL,
    api_name            VARCHAR(255)    NOT NULL,
    execution_order     INTEGER         NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    http_status_code    INTEGER,
    request_payload     TEXT,
    response_payload    TEXT,
    db_validation_result TEXT,
    csv_validation_result TEXT,
    execution_time_ms   BIGINT,
    error_message       TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_journey_run ON journey_execution_history(journey_run_id);
CREATE INDEX IF NOT EXISTS idx_journey_scenario ON journey_execution_history(scenario_code, module_code);
