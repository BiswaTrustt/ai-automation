# API Automation Framework

> **Enterprise-grade, metadata-driven REST API automation platform**  
> Built with Java 17 · Spring Boot 3.x · REST Assured · PostgreSQL · Gradle

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Tech Stack](#tech-stack)
4. [Prerequisites](#prerequisites)
5. [Database Setup](#database-setup)
6. [Configuration](#configuration)
7. [Running the Application](#running-the-application)
8. [Running Tests](#running-tests)
9. [Onboarding a New API](#onboarding-a-new-api)
10. [Dynamic Placeholders](#dynamic-placeholders)
11. [Authentication](#authentication)
12. [REST API](#rest-api)
13. [Project Structure](#project-structure)

---

## Overview

This framework executes REST API tests **without writing any API-specific Java code**.  
All API definitions live in PostgreSQL. The engine resolves placeholders, builds requests,
executes them with REST Assured, validates responses, and persists execution history — all at runtime.

Key benefits:
- **Zero code changes** to onboard new APIs — insert a row in the database.
- **Scales to 1 000+ APIs** with the same runtime footprint.
- **Full audit trail** — every execution is persisted to `api_execution_history`.
- **Environment-agnostic** — base URLs are stored per API and can be overridden via environment variables.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  ApiExecutionService                     │
│  (orchestrator – coordinates all subsystems)            │
└────────┬───────────┬───────────┬──────────┬────────────┘
         │           │           │          │
   ApiMetadata  GenericApi  Response   Execution
     Service    Executor   Validation  History
                           Engine      Service
         │           │
   ┌─────┴──┐   ┌────┴─────────────┐
   │ Header │   │ Authentication   │
   │ Builder│   │ Handler          │
   └─────┬──┘   └────────────────┘
         │
  Placeholder
   Resolver
         │
   PostgreSQL  ←──────────────────  Flyway migrations
```

**Execution flow:**

1. Caller invokes `ApiExecutionService.execute(apiName, dynamicValues)`
2. `ApiMetadataService` fetches master, headers, template, validations from DB
3. `PlaceholderResolver` replaces `${TOKEN}` in headers and body
4. `GenericApiExecutor` builds the REST Assured spec and dispatches the HTTP call
5. `ResponseValidationEngine` evaluates each validation rule
6. `ExecutionHistoryService` persists the outcome to `api_execution_history`
7. `ExecutionResultDto` is returned to the caller

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.x |
| Build | Gradle 8.x |
| HTTP Client | REST Assured 5.4.0 |
| Database | PostgreSQL 15+ |
| ORM | Spring Data JPA / Hibernate 6 |
| Migrations | Flyway |
| Testing | JUnit 5 |
| Utilities | Lombok, Jackson |
| Logging | SLF4J + Logback |

---

## Prerequisites

- **JDK 17+** (e.g. Eclipse Temurin / Amazon Corretto)
- **PostgreSQL 15+** running locally or accessible remotely
- **Gradle 8+** (or use the included Gradle wrapper)

---

## Database Setup

Create the databases before starting:

```sql
-- For the main application
CREATE DATABASE api_automation_db;

-- For tests (optional – only needed if running integration tests)
CREATE DATABASE api_automation_db_test;
```

Flyway automatically applies `V1__create_tables.sql` and `V2__insert_sample_data.sql`
on every application start.

---

## Configuration

Edit `src/main/resources/application.yml` or set environment variables:

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/api_automation_db` | Full JDBC URL |

For test runs, override via `application-test.yml` or:

```bash
export DB_USERNAME=myuser
export DB_PASSWORD=mypassword
```

---

## Running the Application

```bash
# Clone and build
./gradlew build -x test

# Start
./gradlew bootRun

# Or with custom DB
DB_USERNAME=myuser DB_PASSWORD=mypass ./gradlew bootRun
```

The server starts on **port 8080** by default.

---

## Running Tests

```bash
# All tests
./gradlew test

# Single test class
./gradlew test --tests "com.example.apiframework.tests.PlaceholderResolverTest"

# Integration tests (requires running PostgreSQL)
./gradlew test --tests "com.example.apiframework.tests.ApiExecutionIntegrationTest"
```

Test reports: `build/reports/tests/test/index.html`

---

## Onboarding a New API

No Java code required. Insert records in the following order:

### 1. Register the API

```sql
INSERT INTO api_master (api_name, module_name, base_url, endpoint, http_method, auth_type, content_type, active)
VALUES ('myNewApi', 'MyModule', 'https://api.example.com', '/v1/resource', 'POST', 'NONE', 'application/json', TRUE);
```

### 2. Add headers

```sql
WITH api AS (SELECT api_id FROM api_master WHERE api_name = 'myNewApi')
INSERT INTO api_headers (api_id, header_key, header_value, is_dynamic, sequence_no)
SELECT api_id, 'X-Tenant', 'acme', FALSE, 1 FROM api;
```

### 3. Add request body (optional)

```sql
WITH api AS (SELECT api_id FROM api_master WHERE api_name = 'myNewApi')
INSERT INTO api_request_templates (api_id, request_template)
SELECT api_id, '{"id": "${RESOURCE_ID}"}'::jsonb FROM api;
```

### 4. Add validation rules

```sql
WITH api AS (SELECT api_id FROM api_master WHERE api_name = 'myNewApi')
INSERT INTO api_validations (api_id, json_path, expected_value, validation_type, mandatory)
SELECT api_id, v.jp, v.ev, v.vt, v.m
FROM api,
(VALUES (NULL, '200', 'STATUS_CODE', TRUE), ('$.status', 'OK', 'JSON_PATH', FALSE))
    AS v(jp, ev, vt, m);
```

### 5. Execute via Java

```java
ExecutionResultDto result = apiExecutionService.execute(
    "myNewApi",
    Map.of("RESOURCE_ID", "abc-123")
);
```

### 6. Or via REST

```bash
curl -X POST http://localhost:8080/api/v1/execute/myNewApi \
  -H "Content-Type: application/json" \
  -d '{"dynamicValues": {"RESOURCE_ID": "abc-123"}}'
```

---

## Dynamic Placeholders

| Placeholder | Resolved to |
|---|---|
| `${CURRENT_TIMESTAMP}` | `yyyyMMddHHmmssSSS` of current time |
| `${UUID}` | Random UUID without hyphens |
| `${RANDOM_NUMBER}` | Random 9-digit integer |
| `${TODAY}` | `yyyy-MM-dd` of today |
| `${CUSTOMER_ID}` | Caller-supplied value |
| `${MOBILE_NUMBER}` | Caller-supplied value |
| Any `${KEY}` | Resolved from caller's `dynamicValues` map |

---

## Authentication

| `auth_type` | Credentials required |
|---|---|
| `NONE` | None |
| `BASIC` | `BASIC_USERNAME`, `BASIC_PASSWORD` |
| `BEARER` | `BEARER_TOKEN` |
| `API_KEY` | `API_KEY_HEADER`, `API_KEY_VALUE` |
| `OAUTH2` | `OAUTH2_TOKEN` |

Pass credentials as the third argument:

```java
apiExecutionService.execute(apiName, dynamicValues,
    Map.of("BEARER_TOKEN", "eyJhbGci..."));
```

---

## REST API

### Execute an API

```
POST /api/v1/execute/{apiName}
Content-Type: application/json

{
  "dynamicValues": { "CUSTOMER_ID": "501147", "MOBILE_NUMBER": "9816923672" },
  "credentials":   { "BEARER_TOKEN": "optional-token" }
}
```

**Response:**
```json
{
  "executionId": 1,
  "apiName": "getBorrowerLoanApplicationDetailsByCustomerId",
  "statusCode": 200,
  "responseBody": "...",
  "executionStatus": "SUCCESS",
  "executionTimeMs": 342,
  "validationResults": [
    { "validationType": "STATUS_CODE", "passed": true },
    { "validationType": "NOT_NULL",    "passed": true }
  ],
  "executedAt": "2024-01-15T10:30:00",
  "message": "API executed successfully in 342ms with HTTP 200"
}
```

---

## Project Structure

```
src/
├── main/java/com/example/apiframework/
│   ├── ApiAutomationFrameworkApplication.java
│   ├── config/
│   │   ├── JacksonConfig.java
│   │   └── RestAssuredConfiguration.java
│   ├── controller/
│   │   └── ApiExecutionController.java
│   ├── dto/
│   │   ├── ApiMetadataDto.java
│   │   ├── ExecutionResultDto.java
│   │   ├── ValidationResultDto.java
│   │   └── ValidationRuleDto.java
│   ├── entity/
│   │   ├── ApiMaster.java
│   │   ├── ApiHeader.java
│   │   ├── ApiRequestTemplate.java
│   │   ├── ApiValidation.java
│   │   └── ApiExecutionHistory.java
│   ├── enums/
│   │   ├── AuthType.java
│   │   ├── ExecutionStatus.java
│   │   └── ValidationType.java
│   ├── exception/
│   │   ├── ApiExecutionException.java
│   │   ├── ApiNotFoundException.java
│   │   └── GlobalExceptionHandler.java
│   ├── repository/
│   │   ├── ApiMasterRepository.java
│   │   └── ApiExecutionHistoryRepository.java
│   ├── service/
│   │   ├── ApiExecutionService.java        ← top-level orchestrator
│   │   ├── ApiMetadataService.java         ← DB metadata assembly
│   │   ├── AuthenticationHandler.java      ← auth strategies
│   │   ├── ExecutionHistoryService.java    ← audit persistence
│   │   ├── GenericApiExecutor.java         ← REST Assured execution
│   │   └── ResponseValidationEngine.java   ← response assertions
│   └── util/
│       ├── HeaderBuilder.java
│       └── PlaceholderResolver.java
├── main/resources/
│   ├── application.yml
│   ├── application-test.yml
│   ├── logback-spring.xml
│   └── db/migration/
│       ├── V1__create_tables.sql
│       └── V2__insert_sample_data.sql
└── test/java/com/example/apiframework/
    ├── base/
    │   └── BaseIntegrationTest.java
    └── tests/
        ├── ApiExecutionIntegrationTest.java
        └── PlaceholderResolverTest.java
```

---

## Logging

| Logger | Output |
|---|---|
| Application logs | `logs/api-framework.log` + console |
| Execution history | `logs/api-executions.log` |
| Logs roll daily | Kept for 30 days |

---

*Built for scale. Zero code for new APIs. Fully production-ready.*
