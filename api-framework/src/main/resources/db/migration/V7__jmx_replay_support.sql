-- =============================================================
-- V7__jmx_replay_support.sql
-- Adds the columns required to replay a JMeter test plan end-to-end:
--   - per-step loop count (LoopController / WhileController)
--   - per-step delay (Constant Timer)
--   - per-step extraction map (JSON / Regex Post-Processors)
--   - cross-module scenario flag (a single scenario can span every module
--     when the JMX flow walks through multiple TransactionControllers)
-- All defaults are backward compatible: existing scenarios behave unchanged.
-- =============================================================

ALTER TABLE api_scenario_mapping
    ADD COLUMN IF NOT EXISTS loop_count           INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS delay_ms             INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS extraction_mappings  JSONB;

COMMENT ON COLUMN api_scenario_mapping.loop_count
    IS 'Number of times this step is executed (LoopController / WhileController in JMeter; default 1)';
COMMENT ON COLUMN api_scenario_mapping.delay_ms
    IS 'Sleep before this step in ms (JMeter Constant Timer)';
COMMENT ON COLUMN api_scenario_mapping.extraction_mappings
    IS 'JSON object mapping captured-variable name -> JsonPath expression evaluated against the response body';

ALTER TABLE test_scenario_master
    ADD COLUMN IF NOT EXISTS cross_module BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN test_scenario_master.cross_module
    IS 'TRUE when the scenario walks across multiple modules (JMX e2e flow); ModuleService skips the module-name filter for these scenarios';
