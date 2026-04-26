# API Journey Automation Framework

Production-grade, fully database-driven framework for executing end-to-end API
business journeys (QDE → Eligibility → Account Details → DDE → Document Upload
→ Disbursement) on top of Spring Boot 3.3 + Java 21 + REST Assured + TestNG +
Extent Reports + PostgreSQL + Flyway.

The framework was layered on top of the existing single-API runner
(`com.example.apiframework.*`) rather than replacing it — both coexist:

```
src/main/java/com/example/apiframework
├── …                    # original single-API runner (api_master / headers / templates / validations)
└── journey              # NEW journey orchestration layer
    ├── controller       # REST: POST /journey/execute?module=&scenario=
    ├── csv              # CsvExpectedResultReader, CsvResultWriter
    ├── dto              # JourneyContext, JourneyResult, ApiStepResult, ValidationOutcome
    ├── entity           # 6 new entities
    ├── enums            # JourneyStatus
    ├── executor         # JourneyExecutor (public entry point)
    ├── listener         # ExtentReportListener
    ├── repository       # 6 repositories
    ├── service          # 9 services (orchestration)
    └── util             # DynamicValueResolver
```

## Run

```bash
# 1. Postgres at localhost:5432, db `api_automation_db`, user/pass postgres/root
./gradlew clean bootRun     # starts the app + runs migrations V1..V4

# 2. Trigger via REST
curl -X POST 'http://localhost:8080/journey/execute?module=QDE&scenario=VALID_CUSTOMER'

# 3. Or run the TestNG suite (CI-friendly, generates Extent HTML report)
./gradlew clean test
open api-framework/reports/extent-*.html
```

## Java entry point

```java
@Autowired JourneyExecutor journeyExecutor;
JourneyResult result = journeyExecutor.execute("QDE", "VALID_CUSTOMER");
```

## Schema

| Table                       | Purpose                                                |
|-----------------------------|--------------------------------------------------------|
| `api_master`                | (existing) API definition: URL, method, auth           |
| `api_headers`               | (existing) per-API headers, with placeholder support   |
| `api_request_templates`     | (existing) JSON body templates                         |
| `api_validations`           | (existing) per-API response assertions                 |
| `test_scenario_master`      | scenarios per module                                   |
| `api_scenario_mapping`      | which APIs run for a scenario (with order)             |
| `api_pre_sql`               | SQL to run before each API call                        |
| `api_post_validation_sql`   | SQL to run after each API call to fetch actual values  |
| `api_expected_results`      | DB-stored expected values per scenario+api+key         |
| `journey_execution_history` | one row per API step in a journey run (audit trail)    |

## Dynamic placeholders

Anywhere a request body, header, or SQL string contains `${TYPE:arg}`, the
`DynamicValueResolver` substitutes at runtime:

| Token                     | Resolves to                                            |
|---------------------------|--------------------------------------------------------|
| `${RANDOM:N}`             | N-digit random integer (default 10)                    |
| `${RANDOM:UUID}`          | random UUID                                            |
| `${TIMESTAMP}`            | current epoch millis                                   |
| `${TIMESTAMP:yyyy-MM-dd}` | formatted current time                                 |
| `${ENV:key}`              | Spring `Environment` property                          |
| `${DB:select …}`          | first-column-of-first-row from a JdbcTemplate query    |
| `${CSV:file:column}`      | first row column from `classpath:/expected/<file>`     |
| `${RESPONSE:api.$.path}`  | JsonPath into a previously captured response body      |
| `${CTX:key}`              | `JourneyContext.capture(key, …)` value                 |

Flyway placeholder replacement is disabled (`FlywayConfig` + yaml), so `${…}`
inside migration files is left literal and reaches the resolver intact.

## Validation sources

For every step, the journey collects three validation streams:

1. **HTTP status** — pass if 2xx/3xx
2. **DB validation** — for each `api_post_validation_sql` row, run the SQL,
   pull `expected_column`, compare to `api_expected_results.expected_value` for
   the same `(scenario_id, api_id, validation_name = validation_key)`.
3. **CSV validation** — `classpath:/expected/<scenario_code>.csv`, columns
   `scenario_code,api_name,validation_key,expected_value`.
   - keys starting with `$` are JSONPath into the response body
   - other keys look up the `actual` from the DB validation map

A step is `PASS` iff all three streams pass; the journey is `PASS` iff every
step is `PASS`.

## Reports

| Output                                    | Where                                          |
|-------------------------------------------|------------------------------------------------|
| `journey_execution_history` rows          | DB — one per API step                          |
| `reports/results-<module>-<scn>-<ts>.csv` | filesystem (CsvResultWriter)                   |
| `reports/extent-<ts>.html`                | filesystem (ExtentReportListener via TestNG)   |
| TestNG XML/HTML                           | `build/reports/tests/test/`                    |

## Sample seed (V4)

The V4 migration creates `demo_customer` + `demo_loan_application`, registers
three demo APIs (`saveQde`, `fetchEligibility`, `accountDetails`) pointing at
`https://httpbin.org/post`, and sets up a `VALID_CUSTOMER` scenario with
pre-SQL, post-SQL, and expected values — so the framework runs end-to-end
without needing a real backend.

Replace `base_url`/payloads in V4 (or new migrations) to point at any real
environment.

## Adding a new scenario

```sql
-- 1. scenario row
INSERT INTO test_scenario_master(scenario_code, scenario_name, module_code)
VALUES ('INVALID_PAN', 'QDE with malformed PAN', 'QDE');

-- 2. map APIs in order
INSERT INTO api_scenario_mapping(api_id, scenario_id, execution_order)
SELECT api_id, (SELECT id FROM test_scenario_master WHERE scenario_code='INVALID_PAN'), 10
FROM api_master WHERE api_name='saveQde';

-- 3. (optional) pre-SQL, post-SQL, expected_results, CSV
```

No code change required.

## Configuration

`application.yml` — Postgres URL, Flyway, logging.
`application-test.yml` — test database, used by `@SpringBootTest`.

⚠️ Pre/post-SQL is executed verbatim via `JdbcTemplate.execute`. Treat the
journey tables as a privileged surface and never expose them to untrusted
users.
