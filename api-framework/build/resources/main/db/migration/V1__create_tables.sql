-- =============================================================
-- V1__create_tables.sql
-- API Automation Framework - Database Schema
-- =============================================================

-- ---------------------------------------------------------------
-- 1. api_master: Core API metadata
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_master (
    api_id          BIGSERIAL       PRIMARY KEY,
    api_name        VARCHAR(255)    NOT NULL UNIQUE,
    module_name     VARCHAR(100)    NOT NULL,
    base_url        VARCHAR(500)    NOT NULL,
    endpoint        VARCHAR(500)    NOT NULL,
    http_method     VARCHAR(10)     NOT NULL CHECK (http_method IN ('GET','POST','PUT','PATCH','DELETE','HEAD','OPTIONS')),
    auth_type       VARCHAR(20)     NOT NULL DEFAULT 'NONE' CHECK (auth_type IN ('NONE','BASIC','BEARER','API_KEY','OAUTH2')),
    content_type    VARCHAR(100)    NOT NULL DEFAULT 'application/json',
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  api_master               IS 'Master table holding all registered API definitions';
COMMENT ON COLUMN api_master.api_id        IS 'Surrogate primary key';
COMMENT ON COLUMN api_master.api_name      IS 'Unique logical name used to look up the API';
COMMENT ON COLUMN api_master.module_name   IS 'Business module / domain the API belongs to';
COMMENT ON COLUMN api_master.base_url      IS 'Scheme + host (e.g. https://api.example.com)';
COMMENT ON COLUMN api_master.endpoint      IS 'Path portion of the URL (e.g. /v1/users)';
COMMENT ON COLUMN api_master.http_method   IS 'HTTP verb';
COMMENT ON COLUMN api_master.auth_type     IS 'Authentication strategy applied at execution time';
COMMENT ON COLUMN api_master.content_type  IS 'Request Content-Type header value';
COMMENT ON COLUMN api_master.active        IS 'Soft-delete / enable flag';

-- ---------------------------------------------------------------
-- 2. api_headers: Per-API header definitions
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_headers (
    header_id       BIGSERIAL       PRIMARY KEY,
    api_id          BIGINT          NOT NULL REFERENCES api_master(api_id) ON DELETE CASCADE,
    header_key      VARCHAR(200)    NOT NULL,
    header_value    VARCHAR(500)    NOT NULL,
    is_dynamic      BOOLEAN         NOT NULL DEFAULT FALSE,
    sequence_no     INT             NOT NULL DEFAULT 0
);

COMMENT ON TABLE  api_headers              IS 'HTTP headers to attach to every execution of the parent API';
COMMENT ON COLUMN api_headers.is_dynamic   IS 'TRUE when header_value contains a ${PLACEHOLDER}';
COMMENT ON COLUMN api_headers.sequence_no  IS 'Ordering hint (lower = applied first)';

CREATE INDEX IF NOT EXISTS idx_api_headers_api_id ON api_headers(api_id);

-- ---------------------------------------------------------------
-- 3. api_request_templates: JSON body templates
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_request_templates (
    template_id         BIGSERIAL   PRIMARY KEY,
    api_id              BIGINT      NOT NULL REFERENCES api_master(api_id) ON DELETE CASCADE,
    request_template    JSONB       NOT NULL
);

COMMENT ON TABLE  api_request_templates                  IS 'JSON body templates; placeholders are resolved at runtime';
COMMENT ON COLUMN api_request_templates.request_template IS 'JSONB template with ${PLACEHOLDER} tokens';

CREATE INDEX IF NOT EXISTS idx_api_templates_api_id ON api_request_templates(api_id);

-- ---------------------------------------------------------------
-- 4. api_validations: Response assertion rules
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_validations (
    validation_id   BIGSERIAL       PRIMARY KEY,
    api_id          BIGINT          NOT NULL REFERENCES api_master(api_id) ON DELETE CASCADE,
    json_path       VARCHAR(500),
    expected_value  VARCHAR(1000),
    validation_type VARCHAR(50)     NOT NULL CHECK (validation_type IN ('STATUS_CODE','JSON_PATH','NOT_NULL','CONTAINS','REGEX')),
    mandatory       BOOLEAN         NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE  api_validations                  IS 'Response validation rules evaluated after each execution';
COMMENT ON COLUMN api_validations.json_path        IS 'JsonPath expression (e.g. $.data.id); null for STATUS_CODE rules';
COMMENT ON COLUMN api_validations.expected_value   IS 'Expected value for equality / regex / contains checks';
COMMENT ON COLUMN api_validations.validation_type  IS 'Validation strategy';
COMMENT ON COLUMN api_validations.mandatory        IS 'Fail the test if this assertion fails';

CREATE INDEX IF NOT EXISTS idx_api_validations_api_id ON api_validations(api_id);

-- ---------------------------------------------------------------
-- 5. api_execution_history: Audit trail of every API call
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_execution_history (
    execution_id        BIGSERIAL       PRIMARY KEY,
    api_id              BIGINT          NOT NULL REFERENCES api_master(api_id),
    request_payload     TEXT,
    response_payload    TEXT,
    status_code         INT,
    execution_status    VARCHAR(20)     NOT NULL CHECK (execution_status IN ('SUCCESS','FAILURE','ERROR','TIMEOUT')),
    execution_time_ms   BIGINT,
    error_message       TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  api_execution_history                    IS 'Immutable audit log of every API test execution';
COMMENT ON COLUMN api_execution_history.execution_status   IS 'Outcome of the execution attempt';
COMMENT ON COLUMN api_execution_history.execution_time_ms  IS 'Wall-clock duration of the HTTP round-trip in milliseconds';
COMMENT ON COLUMN api_execution_history.error_message      IS 'Exception message when execution_status = ERROR';

CREATE INDEX IF NOT EXISTS idx_exec_history_api_id    ON api_execution_history(api_id);
CREATE INDEX IF NOT EXISTS idx_exec_history_created_at ON api_execution_history(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_exec_history_status    ON api_execution_history(execution_status);
