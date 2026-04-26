-- =============================================================
-- V6__loan_product_seed.sql
-- Seed loan products, product-module mappings, and a JLG-specific
-- product-module-scenario journey on top of the existing VALID_CUSTOMER
-- scenario. The legacy NULL-product rows from V4 remain intact, so
-- journeyExecutor.execute("QDE","VALID_CUSTOMER") still works.
-- =============================================================

-- ---------------------------------------------------------------
-- Loan Products
-- ---------------------------------------------------------------
INSERT INTO loan_product_master (product_code, product_name, description, active) VALUES
    ('JLG',  'Joint Liability Group',           'Group lending product with joint liability',          TRUE),
    ('SHG',  'Self Help Group',                 'Self help group lending',                              TRUE),
    ('INDL', 'Individual Loan',                 'Individual borrower lending',                          TRUE);

-- ---------------------------------------------------------------
-- LOS Modules per Product (full LOS lifecycle for each)
-- ---------------------------------------------------------------
WITH p AS (SELECT id, product_code FROM loan_product_master)
INSERT INTO product_module_mapping (product_id, module_code, module_name, sequence_no, active)
SELECT p.id, m.module_code, m.module_name, m.seq, TRUE
FROM p,
(VALUES
    ('QDE',     'Quick Data Entry',                 10),
    ('ES',      'Eligibility Summary',              20),
    ('HHIE',    'Household Income & Expenses',      30),
    ('AD',      'Account Details',                  40),
    ('DDE',     'Detailed Data Entry',              50),
    ('GFM',     'Group Formation & Management',     60),
    ('GCSA',    'Repayment Group CASA Account',     70),
    ('SBET',    'Schedule BET',                     80),
    ('ABET',    'Accept BET',                       90),
    ('RSBET',   'Reschedule BET',                  100),
    ('CBET',    'Conduct BET',                     110),
    ('CUWRTR',  'Credit Underwriting',             120),
    ('HFCM',    'Hold for Clarification',          130),
    ('CMD',     'CM Dashboard',                    140),
    ('DOTAC',   'Dot Account Capture',             150),
    ('DACVM',   'Verify Dot Account',              160),
    ('ESGN',    'E-Sign',                          170),
    ('PLD',     'Print Loan Documents',            180),
    ('PSGN',    'Physical Sign',                   190),
    ('CPDC_M',  'Conduct PDC',                     200)
) AS m(module_code, module_name, seq)
WHERE p.product_code IN ('JLG','SHG','INDL');

-- ---------------------------------------------------------------
-- Product-aware API journey: JLG + QDE + VALID_CUSTOMER
-- Reuses the same APIs as the legacy mapping but tagged with product_id.
-- ---------------------------------------------------------------
WITH s   AS (SELECT id FROM test_scenario_master WHERE scenario_code = 'VALID_CUSTOMER'),
     pr  AS (SELECT id FROM loan_product_master  WHERE product_code  = 'JLG'),
     a1  AS (SELECT api_id FROM api_master WHERE api_name = 'saveQde'),
     a2  AS (SELECT api_id FROM api_master WHERE api_name = 'fetchEligibility'),
     a3  AS (SELECT api_id FROM api_master WHERE api_name = 'accountDetails')
INSERT INTO api_scenario_mapping (api_id, scenario_id, product_id, execution_order, active)
SELECT a1.api_id, s.id, pr.id, 10, TRUE FROM s, pr, a1
UNION ALL
SELECT a2.api_id, s.id, pr.id, 20, TRUE FROM s, pr, a2
UNION ALL
SELECT a3.api_id, s.id, pr.id, 30, TRUE FROM s, pr, a3;

-- A SHG-flavored QDE journey runs only saveQde (different sequence per product).
WITH s   AS (SELECT id FROM test_scenario_master WHERE scenario_code = 'VALID_CUSTOMER'),
     pr  AS (SELECT id FROM loan_product_master  WHERE product_code  = 'SHG'),
     a1  AS (SELECT api_id FROM api_master WHERE api_name = 'saveQde')
INSERT INTO api_scenario_mapping (api_id, scenario_id, product_id, execution_order, active)
SELECT a1.api_id, s.id, pr.id, 10, TRUE FROM s, pr, a1;
