-- =============================================================
-- V4__journey_seed_data.sql
-- Sample QDE journey using httpbin.org so the framework runs end-to-end
-- without depending on a real backend. Replace base_url + payloads to
-- point at any real environment.
-- =============================================================

-- ---------------------------------------------------------------
-- 1. Sample tables exercised by pre-SQL / post-validation SQL.
--    Created up-front so the framework demo runs against a real schema.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demo_customer (
    id          BIGSERIAL   PRIMARY KEY,
    mobile_no   VARCHAR(20) NOT NULL,
    full_name   VARCHAR(255),
    pan         VARCHAR(20),
    created_at  TIMESTAMP   DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS demo_loan_application (
    id          BIGSERIAL   PRIMARY KEY,
    mobile_no   VARCHAR(20) NOT NULL,
    loan_status VARCHAR(50) NOT NULL,
    amount      NUMERIC(15,2),
    created_at  TIMESTAMP   DEFAULT NOW()
);

-- ---------------------------------------------------------------
-- 2. Journey APIs (saveQde, fetchEligibility, accountDetails)
-- ---------------------------------------------------------------
INSERT INTO api_master (api_name, module_name, base_url, endpoint, http_method, auth_type, content_type, active) VALUES
    ('saveQde',          'QDE', 'https://httpbin.org', '/post', 'POST', 'NONE', 'application/json', TRUE),
    ('fetchEligibility', 'QDE', 'https://httpbin.org', '/post', 'POST', 'NONE', 'application/json', TRUE),
    ('accountDetails',   'QDE', 'https://httpbin.org', '/post', 'POST', 'NONE', 'application/json', TRUE);

-- ---------------------------------------------------------------
-- 3. Request templates (placeholders are journey-framework, not Flyway)
--    The journey resolver consumes ${RANDOM:N}, ${TIMESTAMP}, ${RESPONSE:...},
--    ${ENV:...}, ${DB:...}, ${CSV:...} at runtime.
-- ---------------------------------------------------------------
WITH api AS (SELECT api_id FROM api_master WHERE api_name = 'saveQde')
INSERT INTO api_request_templates (api_id, request_template)
SELECT api_id,
    '{"mobileNumber":"9999999999","customerType":"NEW","fullName":"Demo Customer","pan":"ABCDE1234F","timestamp":"${TIMESTAMP}","stan":"${RANDOM:12}"}'::jsonb
FROM api;

WITH api AS (SELECT api_id FROM api_master WHERE api_name = 'fetchEligibility')
INSERT INTO api_request_templates (api_id, request_template)
SELECT api_id,
    '{"mobileNumber":"9999999999","priorRequestId":"${RESPONSE:saveQde.$.json.stan}"}'::jsonb
FROM api;

WITH api AS (SELECT api_id FROM api_master WHERE api_name = 'accountDetails')
INSERT INTO api_request_templates (api_id, request_template)
SELECT api_id,
    '{"mobileNumber":"9999999999","accountType":"SAVINGS"}'::jsonb
FROM api;

-- Note: Flyway placeholder replacement is disabled in this project
-- (FlywayConfig + application.yml), so ${...} is stored literally and
-- the journey DynamicValueResolver picks it up at runtime.

-- ---------------------------------------------------------------
-- 4. Test Scenario
-- ---------------------------------------------------------------
INSERT INTO test_scenario_master (scenario_code, scenario_name, module_code, description, active) VALUES
    ('VALID_CUSTOMER',   'Happy-path QDE journey for a new customer',          'QDE', 'New customer onboarding through QDE', TRUE),
    ('SCENARIO_001',     'Alias scenario referenced in product spec',          'QDE', 'Same as VALID_CUSTOMER',              TRUE);

-- ---------------------------------------------------------------
-- 5. API <-> Scenario mapping (execution order)
-- ---------------------------------------------------------------
WITH s AS (SELECT id FROM test_scenario_master WHERE scenario_code = 'VALID_CUSTOMER'),
     a1 AS (SELECT api_id FROM api_master WHERE api_name = 'saveQde'),
     a2 AS (SELECT api_id FROM api_master WHERE api_name = 'fetchEligibility'),
     a3 AS (SELECT api_id FROM api_master WHERE api_name = 'accountDetails')
INSERT INTO api_scenario_mapping (api_id, scenario_id, execution_order, active)
SELECT a1.api_id, s.id, 10, TRUE FROM s, a1
UNION ALL
SELECT a2.api_id, s.id, 20, TRUE FROM s, a2
UNION ALL
SELECT a3.api_id, s.id, 30, TRUE FROM s, a3;

WITH s AS (SELECT id FROM test_scenario_master WHERE scenario_code = 'SCENARIO_001'),
     a1 AS (SELECT api_id FROM api_master WHERE api_name = 'saveQde')
INSERT INTO api_scenario_mapping (api_id, scenario_id, execution_order, active)
SELECT a1.api_id, s.id, 10, TRUE FROM s, a1;

-- ---------------------------------------------------------------
-- 6. Pre-SQL: clean stale data before saveQde
-- ---------------------------------------------------------------
WITH s AS (SELECT id FROM test_scenario_master WHERE scenario_code = 'VALID_CUSTOMER'),
     a AS (SELECT api_id FROM api_master WHERE api_name = 'saveQde')
INSERT INTO api_pre_sql (api_id, scenario_id, sql_order, sql_query, active)
SELECT a.api_id, s.id, 10,
       'DELETE FROM demo_customer WHERE mobile_no = ''9999999999''', TRUE FROM s, a
UNION ALL
SELECT a.api_id, s.id, 20,
       'DELETE FROM demo_loan_application WHERE mobile_no = ''9999999999''', TRUE FROM s, a
UNION ALL
SELECT a.api_id, s.id, 30,
       'INSERT INTO demo_loan_application (mobile_no, loan_status, amount) VALUES (''9999999999'', ''APPROVED'', 50000.00)', TRUE FROM s, a;

-- ---------------------------------------------------------------
-- 7. Post-validation SQL: fetch actual values from DB
-- ---------------------------------------------------------------
WITH s AS (SELECT id FROM test_scenario_master WHERE scenario_code = 'VALID_CUSTOMER'),
     a AS (SELECT api_id FROM api_master WHERE api_name = 'saveQde')
INSERT INTO api_post_validation_sql (api_id, scenario_id, validation_name, sql_query, expected_column, active)
SELECT a.api_id, s.id, 'loan_status',
       'SELECT loan_status FROM demo_loan_application WHERE mobile_no = ''9999999999'' LIMIT 1',
       'loan_status', TRUE FROM s, a;

-- ---------------------------------------------------------------
-- 8. Expected results
-- ---------------------------------------------------------------
WITH s AS (SELECT id FROM test_scenario_master WHERE scenario_code = 'VALID_CUSTOMER'),
     a AS (SELECT api_id FROM api_master WHERE api_name = 'saveQde')
INSERT INTO api_expected_results (scenario_id, api_id, validation_key, expected_value)
SELECT s.id, a.api_id, 'loan_status', 'APPROVED' FROM s, a;
