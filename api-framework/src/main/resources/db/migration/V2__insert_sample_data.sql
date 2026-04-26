-- =============================================================
-- V2__insert_sample_data.sql
-- Sample metadata for getBorrowerLoanApplicationDetailsByCustomerId
-- =============================================================

-- ---------------------------------------------------------------
-- 1. API Master
-- ---------------------------------------------------------------
INSERT INTO api_master (
    api_name,
    module_name,
    base_url,
    endpoint,
    http_method,
    auth_type,
    content_type,
    active
) VALUES (
    'getBorrowerLoanApplicationDetailsByCustomerId',
    'LoanManagement',
    'https://qa2-mfi.novopay.in',
    '/api-gateway/api/v1/getBorrowerLoanApplicationDetailsByCustomerId',
    'POST',
    'NONE',
    'application/json',
    TRUE
);

-- ---------------------------------------------------------------
-- 2. API Headers
-- (captured by api_name join for portability across environments)
-- ---------------------------------------------------------------
WITH api AS (SELECT api_id FROM api_master WHERE api_name = 'getBorrowerLoanApplicationDetailsByCustomerId')
INSERT INTO api_headers (api_id, header_key, header_value, is_dynamic, sequence_no)
SELECT api.api_id, h.header_key, h.header_value, h.is_dynamic, h.seq
FROM api,
(VALUES
    ('tenant_code',            'mfi',                    FALSE,  1),
    ('user_id',                '171',                    FALSE,  2),
    ('client_code',            'NOVOPAY',                FALSE,  3),
    ('channel_code',           'NOVOPAY',                FALSE,  4),
    ('end_channel_code',       'NOVOPAY',                FALSE,  5),
    ('stan',                   '${CURRENT_TIMESTAMP}',   TRUE,   6),
    ('transmission_datetime',  '${CURRENT_TIMESTAMP}',   TRUE,   7),
    ('operation_mode',         'SELF',                   FALSE,  8),
    ('run_mode',               'REAL',                   FALSE,  9),
    ('actor_type',             'CUSTOMER',               FALSE, 10),
    ('user_handle_type',       'MSISDN',                 FALSE, 11),
    ('user_handle_value',      '${MOBILE_NUMBER}',       TRUE,  12),
    ('function_code',          'DEFAULT',                FALSE, 13),
    ('function_sub_code',      'DEFAULT',                FALSE, 14)
) AS h(header_key, header_value, is_dynamic, seq);

-- ---------------------------------------------------------------
-- 3. Request Template
-- ---------------------------------------------------------------
WITH api AS (SELECT api_id FROM api_master WHERE api_name = 'getBorrowerLoanApplicationDetailsByCustomerId')
INSERT INTO api_request_templates (api_id, request_template)
SELECT api_id,
    '{"request":{"search_criteria":{},"page_size":"10","offset":"0","sort_criteria":{},"customer_id":"${CUSTOMER_ID}"}}'::jsonb
FROM api;

-- ---------------------------------------------------------------
-- 4. Validation Rules
-- ---------------------------------------------------------------
WITH api AS (SELECT api_id FROM api_master WHERE api_name = 'getBorrowerLoanApplicationDetailsByCustomerId')
INSERT INTO api_validations (api_id, json_path, expected_value, validation_type, mandatory)
SELECT api_id, v.json_path, v.expected_value, v.validation_type, v.mandatory
FROM api,
(VALUES
    (NULL,   '200',  'STATUS_CODE', TRUE),
    ('$',    NULL,   'NOT_NULL',    TRUE)
) AS v(json_path, expected_value, validation_type, mandatory);

-- ---------------------------------------------------------------
-- Additional sample APIs for demonstration
-- ---------------------------------------------------------------

-- Sample GET API
INSERT INTO api_master (
    api_name, module_name, base_url, endpoint,
    http_method, auth_type, content_type, active
) VALUES (
    'getHealthCheck',
    'Infrastructure',
    'https://qa2-mfi.novopay.in',
    '/api-gateway/health',
    'GET', 'NONE', 'application/json', TRUE
);

WITH api AS (SELECT api_id FROM api_master WHERE api_name = 'getHealthCheck')
INSERT INTO api_validations (api_id, json_path, expected_value, validation_type, mandatory)
SELECT api_id, v.json_path, v.expected_value, v.validation_type, v.mandatory
FROM api,
(VALUES
    (NULL,    '200',   'STATUS_CODE', TRUE),
    ('$.status', 'UP', 'JSON_PATH',  FALSE)
) AS v(json_path, expected_value, validation_type, mandatory);
