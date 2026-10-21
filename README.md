<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(max-width: 640px) and (prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-dark.svg">
  <source media="(max-width: 640px)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Domain Lending - Regulatory Compliance" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Domain Lending - Regulatory Compliance

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

Domain layer microservice for AML cases within the Firefly platform. This service orchestrates create, update, status change, close, and query against `core-lending-regulatory-compliance` with compensating saga steps.

Repository: [https://github.com/fireflysoftwaresolutions/domain-lending-regulatory-compliance](https://github.com/fireflysoftwaresolutions/domain-lending-regulatory-compliance)

---

## Overview

`domain-lending-regulatory-compliance` sits between experience services and `core-lending-regulatory-compliance`. Write operations run through the FireflyFramework saga engine. Queries use the CQRS query bus. The domain depends on `core-lending-regulatory-compliance-sdk` only. It does not import another core's internal packages. Identifiers that point at parties, contracts, or products stay UUIDs owned by those cores.

It creates, updates, changes status, and closes AML cases through `core-lending-regulatory-compliance`. SARs, actions, reporting runs, submissions, and reporting records stay on the core. This saga covers the AML case.

### Key features

- **Create** -- register a AML case through `core-lending-regulatory-compliance`, with compensation that deletes the core write
- **Update** -- read the current AML case and write the new field values through `update3` on `core-lending-regulatory-compliance`. Status is left unchanged. Compensation writes the previous record back.
- **Status change** -- read the current AML case and update its status through `core-lending-regulatory-compliance`. The regulatory compliance core has no AML case status history resource, so the status saga does not append one.
- **Close** -- Close runs the status saga and writes CLOSED. The core has no cancel status. Remove deletes the case, which the core allows for every status. **Close** writes `CLOSED` when the core lifecycle allows that move.
- **Query** -- read one AML case by identifier
- **Compensation** -- a failed later step asks the core to undo the work already accepted
- **CQRS** -- commands and queries travel on separate buses
- **Reactive I/O** -- Spring WebFlux with virtual threads enabled

## Architecture

### Module structure

| Module | Purpose |
|--------|---------|
| `domain-lending-regulatory-compliance-core` | Saga workflows, commands, handlers, and the application service |
| `domain-lending-regulatory-compliance-interfaces` | Request contracts for a status change and a closing reason |
| `domain-lending-regulatory-compliance-infra` | SDK client factory and downstream base-path configuration |
| `domain-lending-regulatory-compliance-web` | Spring Boot application, REST API, actuator, and OpenAPI |
| `domain-lending-regulatory-compliance-sdk` | Java client generated from the OpenAPI document |

### Dependency flow

```
web --> interfaces --> core --> infra --> core-lending-regulatory-compliance-sdk
sdk (generated from openapi.yml)
```

### Saga map

| Saga | Core calls | Compensation |
|------|------------|--------------|
| `RegisterAmlCaseSaga` | `create3` on `AmlCaseApi` | Deletes the record |
| `UpdateAmlCaseSaga` | Reads the current record, then writes field changes through `update3` on `AmlCaseApi`. Status is left unchanged. | Writes the previous record back through `update3` |
| `UpdateAmlCaseStatusSaga` | Reads the current record, updates its status through `update3` | Writes the previous status back through the core |
| Query | `getById3` on `AmlCaseApi` | None. A read has nothing to undo. |

Status values accepted by the core: `OPEN`, `INVESTIGATING`, `REPORTED`, `CLOSED`, `ESCALATED`.

## Setup

### Prerequisites

- Java 25
- Apache Maven 3.9+
- `core-lending-regulatory-compliance-sdk` 26.10.4
- Apache Kafka on `localhost:9092` when event publishing is left at the default

### Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_ADDRESS` | `localhost` | Bind address |
| `SERVER_PORT` | `8050` | HTTP port |
| `ENDPOINT_CORE_LENDING_COMPLIANCE` | `http://localhost:8080` | Base URL of `core-lending-regulatory-compliance` |

### Build and test

```bash
mvn clean verify
```

### Run

```bash
ENDPOINT_CORE_LENDING_COMPLIANCE=http://localhost:8080 mvn spring-boot:run -pl domain-lending-regulatory-compliance-web
```

Swagger UI (non-production): [http://localhost:8050/swagger-ui.html](http://localhost:8050/swagger-ui.html)

## API

| Method | Path | Behavior |
|--------|------|----------|
| `POST` | `/api/v1/aml-cases` | Runs `RegisterAmlCaseSaga` |
| `GET` | `/api/v1/aml-cases/{amlCaseId}` | Queries the core |
| `PUT` | `/api/v1/aml-cases/{amlCaseId}/status` | Runs `UpdateAmlCaseStatusSaga` |
| `PUT` | `/api/v1/aml-cases/{amlCaseId}` | Runs `UpdateAmlCaseSaga` |
| `POST` | `/api/v1/aml-cases/{amlCaseId}/close` | Close AML case. Runs the status saga when `core-lending-regulatory-compliance` allows `CLOSED`. |
| `DELETE` | `/api/v1/aml-cases/{amlCaseId}` | Deletes the record through `core-lending-regulatory-compliance` when that core allows removal |

The status request body is `status` plus an optional `statusReason`.

Close writes CLOSED. The regulatory compliance core does not restrict which AML case status can follow another and has no cancel status. Remove deletes any AML case because the core delete has no status guard.
A closing command body is an optional `statusReason`.
